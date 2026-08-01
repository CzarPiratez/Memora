package com.memora.app.data.notes

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.memora.app.application.notes.OneNoteAuthOutcome
import com.memora.app.application.notes.OneNoteInteractiveAuth
import com.memora.app.domain.notes.NotesProviderSession
import com.memora.app.domain.notes.NotesProviderTokenVault
import com.memora.app.domain.notes.OneNoteAuthConfiguration
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUserCancelException
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * MSAL single-account auth for OneNote. Tokens are mirrored into
 * [NotesProviderTokenVault] for Memora-owned disconnect/clear semantics.
 *
 * First connect uses [ISingleAccountPublicClientApplication.signIn]. Reconnect
 * uses silent acquire; account-mismatch clears the session and signs in again.
 */
class MsalOneNoteInteractiveAuth(
    context: Context,
    private val configuration: OneNoteAuthConfiguration,
    private val tokenVault: NotesProviderTokenVault,
) : OneNoteInteractiveAuth {
    private val appContext = context.applicationContext

    override suspend fun connect(activity: Activity): OneNoteAuthOutcome {
        if (!configuration.isRegistrationConfigured) {
            return OneNoteAuthOutcome.RegistrationRequired
        }
        return try {
            var app = obtainApplication()
            var account = currentAccount(app)
            val attempt = if (account == null) {
                signInInteractive(activity, app)
            } else {
                when (val silent = acquireTokenSilent(app, account)) {
                    is TokenAttempt.Success -> silent
                    else -> {
                        val interactive = signInInteractive(activity, app)
                        if (interactive is TokenAttempt.Failed && isAccountMismatch(interactive.message)) {
                            signOut(app)
                            tokenVault.clearSession()
                            app = obtainApplication()
                            signInInteractive(activity, app)
                        } else {
                            interactive
                        }
                    }
                }
            }
            when (attempt) {
                is TokenAttempt.Success -> {
                    val session = toSession(attempt.auth)
                    withContext(Dispatchers.IO) { tokenVault.writeSession(session) }
                    OneNoteAuthOutcome.Connected(session)
                }
                TokenAttempt.Cancelled -> OneNoteAuthOutcome.Cancelled
                is TokenAttempt.Failed -> {
                    if (isAccountMismatch(attempt.message)) {
                        signOut(app)
                        tokenVault.clearSession()
                        app = obtainApplication()
                        when (val retry = signInInteractive(activity, app)) {
                            is TokenAttempt.Success -> {
                                val session = toSession(retry.auth)
                                withContext(Dispatchers.IO) { tokenVault.writeSession(session) }
                                OneNoteAuthOutcome.Connected(session)
                            }
                            TokenAttempt.Cancelled -> OneNoteAuthOutcome.Cancelled
                            is TokenAttempt.Failed -> OneNoteAuthOutcome.Failed(
                                "Microsoft sign-in hit an account conflict. " +
                                    "Tap Connect OneNote once more and choose your Microsoft account.",
                            )
                        }
                    } else {
                        OneNoteAuthOutcome.Failed(attempt.message)
                    }
                }
            }
        } catch (error: Exception) {
            OneNoteAuthOutcome.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Microsoft sign-in failed. Try again when you have a network connection.",
            )
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching {
                if (configuration.isRegistrationConfigured) {
                    signOut(obtainApplication())
                }
            }
            tokenVault.clearSession()
        }
    }

    override suspend fun restoreAccountLabel(): String? = withContext(Dispatchers.IO) {
        tokenVault.readSession()?.accountDisplayLabel
            ?: runCatching {
                if (!configuration.isRegistrationConfigured) return@runCatching null
                currentAccount(obtainApplication())?.username?.takeIf { it.isNotBlank() }
            }.getOrNull()
    }

    private suspend fun obtainApplication(): ISingleAccountPublicClientApplication {
        val configFile = writeConfigFile()
        return suspendCancellableCoroutine { cont ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                appContext,
                configFile,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        if (cont.isActive) cont.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        if (cont.isActive) cont.resumeWithException(exception)
                    }
                },
            )
        }
    }

    private fun writeConfigFile(): File {
        val redirectUri = "msauth://${OneNoteAuthConfiguration.PACKAGE_NAME}/" +
            Uri.encode(configuration.signatureHash)
        val json = JSONObject()
            .put("client_id", configuration.clientId)
            .put("authorization_user_agent", "DEFAULT")
            .put("redirect_uri", redirectUri)
            .put("account_mode", "SINGLE")
            .put("broker_redirect_uri_registered", false)
            .put(
                "authorities",
                JSONArray().put(
                    JSONObject()
                        .put("type", "AAD")
                        .put(
                            "audience",
                            JSONObject()
                                .put("type", "AzureADandPersonalMicrosoftAccount")
                                .put("tenant_id", "common"),
                        ),
                ),
            )
            .toString()
        val file = File(appContext.noBackupFilesDir, MSAL_CONFIG_FILE)
        file.parentFile?.mkdirs()
        file.writeText(json)
        return file
    }

    private suspend fun currentAccount(
        app: ISingleAccountPublicClientApplication,
    ): IAccount? = suspendCancellableCoroutine { cont ->
        app.getCurrentAccountAsync(
            object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    if (cont.isActive) cont.resume(activeAccount)
                }

                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                    if (cont.isActive) cont.resume(currentAccount)
                }

                override fun onError(exception: MsalException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            },
        )
    }

    private suspend fun acquireTokenSilent(
        app: ISingleAccountPublicClientApplication,
        account: IAccount,
    ): TokenAttempt? = suspendCancellableCoroutine { cont ->
        val authority = account.authority ?: DEFAULT_AUTHORITY
        val parameters = AcquireTokenSilentParameters.Builder()
            .withScopes(SCOPES.toList())
            .forAccount(account)
            .fromAuthority(authority)
            .withCallback(
                object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        if (cont.isActive) cont.resume(TokenAttempt.Success(authenticationResult))
                    }

                    override fun onError(exception: MsalException) {
                        if (cont.isActive) {
                            cont.resume(
                                if (isAccountMismatch(exception.message)) {
                                    TokenAttempt.Failed(exception.message ?: ACCOUNT_MISMATCH)
                                } else {
                                    null
                                },
                            )
                        }
                    }
                },
            )
            .build()
        app.acquireTokenSilentAsync(parameters)
    }

    private suspend fun signInInteractive(
        activity: Activity,
        app: ISingleAccountPublicClientApplication,
    ): TokenAttempt = suspendCancellableCoroutine { cont ->
        val callback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                if (cont.isActive) cont.resume(TokenAttempt.Success(authenticationResult))
            }

            override fun onError(exception: MsalException) {
                if (!cont.isActive) return
                cont.resume(
                    when (exception) {
                        is MsalUserCancelException -> TokenAttempt.Cancelled
                        is MsalClientException -> TokenAttempt.Failed(
                            exception.message
                                ?: "Microsoft sign-in could not start on this device.",
                        )
                        else -> TokenAttempt.Failed(
                            exception.message
                                ?: "Microsoft sign-in failed. Check your network and try again.",
                        )
                    },
                )
            }

            override fun onCancel() {
                if (cont.isActive) cont.resume(TokenAttempt.Cancelled)
            }
        }
        val parameters = SignInParameters.builder()
            .withActivity(activity)
            .withScopes(SCOPES.toList())
            .withCallback(callback)
            .build()
        app.signIn(parameters)
    }

    private suspend fun signOut(app: ISingleAccountPublicClientApplication) {
        suspendCancellableCoroutine { cont ->
            app.signOut(
                object : ISingleAccountPublicClientApplication.SignOutCallback {
                    override fun onSignOut() {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(exception: MsalException) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                },
            )
        }
    }

    private fun toSession(result: IAuthenticationResult): NotesProviderSession {
        val account = result.account
        val label = account.username?.takeIf { it.isNotBlank() }
            ?: account.id
            ?: "Microsoft account"
        val accountId = account.id ?: label
        val expires = result.expiresOn?.time
            ?: (System.currentTimeMillis() + DEFAULT_TOKEN_TTL_MS)
        return NotesProviderSession(
            providerId = NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE,
            accountId = accountId,
            accountDisplayLabel = label,
            accessToken = result.accessToken,
            accessTokenExpiresAtEpochMs = expires,
        )
    }

    private fun isAccountMismatch(message: String?): Boolean {
        val text = message?.lowercase().orEmpty()
        return text.contains("does not match") ||
            text.contains(MsalClientException.CURRENT_ACCOUNT_MISMATCH.lowercase())
    }

    private sealed interface TokenAttempt {
        data class Success(val auth: IAuthenticationResult) : TokenAttempt
        data object Cancelled : TokenAttempt
        data class Failed(val message: String) : TokenAttempt
    }

    companion object {
        private const val MSAL_CONFIG_FILE = "msal_onenote_config.json"
        private const val DEFAULT_TOKEN_TTL_MS = 3_600_000L
        private const val DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common"
        private const val ACCOUNT_MISMATCH =
            "The signed in account does not match with the provided account."
        val SCOPES = arrayOf(
            "User.Read",
            "Notes.Read",
            "offline_access",
        )
    }
}
