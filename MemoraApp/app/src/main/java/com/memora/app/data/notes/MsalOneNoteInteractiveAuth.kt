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
import com.microsoft.identity.client.exception.MsalDeclinedScopeException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUserCancelException
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * MSAL single-account auth for OneNote. Tokens are mirrored into
 * [NotesProviderTokenVault] for Memora-owned disconnect/clear semantics.
 *
 * First connect uses [ISingleAccountPublicClientApplication.signIn]. When an
 * MSAL account already exists, uses silent acquire then [signInAgain] — never
 * [signIn] again (that returns "An account is already signed in.").
 *
 * Reuses one [ISingleAccountPublicClientApplication] instance so browser/WebView
 * redirects still match an in-progress interactive call.
 */
class MsalOneNoteInteractiveAuth(
    context: Context,
    private val configuration: OneNoteAuthConfiguration,
    private val tokenVault: NotesProviderTokenVault,
) : OneNoteInteractiveAuth {
    private val appContext = context.applicationContext
    private val applicationMutex = Mutex()
    private var cachedApplication: ISingleAccountPublicClientApplication? = null

    override suspend fun connect(activity: Activity): OneNoteAuthOutcome {
        if (!configuration.isRegistrationConfigured) {
            return OneNoteAuthOutcome.RegistrationRequired
        }
        return try {
            var app = obtainApplication()
            val account = currentAccount(app)
            val attempt = if (account == null) {
                signInInteractive(activity, app)
            } else {
                when (val silent = acquireTokenSilent(app, account)) {
                    is TokenAttempt.Success -> silent
                    is TokenAttempt.Failed -> {
                        if (isAccountMismatch(silent.message)) {
                            signOut(app)
                            tokenVault.clearSession()
                            app = obtainApplication()
                            signInInteractive(activity, app)
                        } else {
                            signInAgainInteractive(activity, app)
                        }
                    }
                    TokenAttempt.Cancelled, null -> signInAgainInteractive(activity, app)
                }
            }
            outcomeFromAttempt(activity, app, attempt)
        } catch (error: Exception) {
            OneNoteAuthOutcome.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Microsoft sign-in failed. Try again when you have a network connection.",
            )
        }
    }

    private suspend fun outcomeFromAttempt(
        activity: Activity,
        app: ISingleAccountPublicClientApplication,
        attempt: TokenAttempt,
    ): OneNoteAuthOutcome {
        var client = app
        return when (attempt) {
            is TokenAttempt.Success -> persistConnected(attempt.auth)
            TokenAttempt.Cancelled -> OneNoteAuthOutcome.Cancelled
            is TokenAttempt.Failed -> {
                when {
                    isAlreadySignedIn(attempt.message) -> {
                        when (val retry = signInAgainInteractive(activity, client)) {
                            is TokenAttempt.Success -> persistConnected(retry.auth)
                            TokenAttempt.Cancelled -> OneNoteAuthOutcome.Cancelled
                            is TokenAttempt.Failed -> {
                                signOut(client)
                                tokenVault.clearSession()
                                client = obtainApplication()
                                when (val fresh = signInInteractive(activity, client)) {
                                    is TokenAttempt.Success -> persistConnected(fresh.auth)
                                    TokenAttempt.Cancelled -> OneNoteAuthOutcome.Cancelled
                                    is TokenAttempt.Failed -> OneNoteAuthOutcome.Failed(fresh.message)
                                }
                            }
                        }
                    }
                    isAccountMismatch(attempt.message) -> {
                        signOut(client)
                        tokenVault.clearSession()
                        client = obtainApplication()
                        when (val retry = signInInteractive(activity, client)) {
                            is TokenAttempt.Success -> persistConnected(retry.auth)
                            TokenAttempt.Cancelled -> OneNoteAuthOutcome.Cancelled
                            is TokenAttempt.Failed -> OneNoteAuthOutcome.Failed(
                                "Microsoft sign-in hit an account conflict. " +
                                    "Tap Connect OneNote once more and choose your Microsoft account.",
                            )
                        }
                    }
                    else -> OneNoteAuthOutcome.Failed(attempt.message)
                }
            }
        }
    }

    private suspend fun persistConnected(auth: IAuthenticationResult): OneNoteAuthOutcome {
        val session = toSession(auth)
        withContext(Dispatchers.IO) { tokenVault.writeSession(session) }
        return OneNoteAuthOutcome.Connected(session)
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
        tokenVault.readSession()
            ?.takeIf { it.accessToken.isNotBlank() }
            ?.accountDisplayLabel
    }

    override suspend fun ensureSession(forceRefresh: Boolean): NotesProviderSession? =
        withContext(Dispatchers.IO) {
            if (!configuration.isRegistrationConfigured) return@withContext null
            val existing = tokenVault.readSession()?.takeIf { it.accessToken.isNotBlank() }
            if (!forceRefresh && existing != null && !isExpiredOrNearExpiry(existing)) {
                return@withContext existing
            }
            refreshSessionSilently() ?: existing?.takeUnless { forceRefresh || isExpiredOrNearExpiry(it) }
        }

    private suspend fun refreshSessionSilently(): NotesProviderSession? {
        return runCatching {
            val app = obtainApplication()
            val account = currentAccount(app) ?: return@runCatching null
            when (val silent = acquireTokenSilent(app, account)) {
                is TokenAttempt.Success -> {
                    val session = toSession(silent.auth)
                    tokenVault.writeSession(session)
                    session
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun isExpiredOrNearExpiry(session: NotesProviderSession): Boolean {
        val skewMs = TOKEN_EXPIRY_SKEW_MS
        return session.accessTokenExpiresAtEpochMs <= System.currentTimeMillis() + skewMs
    }

    private suspend fun obtainApplication(): ISingleAccountPublicClientApplication {
        cachedApplication?.let { return it }
        return applicationMutex.withLock {
            cachedApplication?.let { return it }
            val created = createApplication()
            cachedApplication = created
            created
        }
    }

    private suspend fun createApplication(): ISingleAccountPublicClientApplication {
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
            // WEBVIEW keeps interactive Connect inside Memora and avoids BrowserTabActivity
            // "no interactive call in progress" when the external browser handoff desyncs.
            .put("authorization_user_agent", "WEBVIEW")
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
                        if (!cont.isActive) return
                        cont.resume(
                            when {
                                exception is MsalDeclinedScopeException ->
                                    TokenAttempt.Failed(messageForDeclinedScopes(exception))
                                isAccountMismatch(exception.message) ->
                                    TokenAttempt.Failed(exception.message ?: ACCOUNT_MISMATCH)
                                else -> null
                            },
                        )
                    }
                },
            )
            .build()
        app.acquireTokenSilentAsync(parameters)
    }

    private suspend fun signInInteractive(
        activity: Activity,
        app: ISingleAccountPublicClientApplication,
    ): TokenAttempt = runSignInParameters(activity) { parameters ->
        app.signIn(parameters)
    }

    /**
     * Interactive token for an account that is already present in single-account mode.
     * Calling [ISingleAccountPublicClientApplication.signIn] here fails with
     * "An account is already signed in."
     */
    private suspend fun signInAgainInteractive(
        activity: Activity,
        app: ISingleAccountPublicClientApplication,
    ): TokenAttempt = runSignInParameters(activity) { parameters ->
        app.signInAgain(parameters)
    }

    private suspend fun runSignInParameters(
        activity: Activity,
        invoke: (SignInParameters) -> Unit,
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
                        is MsalDeclinedScopeException ->
                            TokenAttempt.Failed(messageForDeclinedScopes(exception))
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
        invoke(parameters)
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

    private fun isAlreadySignedIn(message: String?): Boolean {
        val text = message?.lowercase().orEmpty()
        return text.contains("account is already signed in")
    }

    private fun messageForDeclinedScopes(exception: MsalDeclinedScopeException): String {
        val declined = exception.declinedScopes.orEmpty().joinToString(", ")
        val granted = exception.grantedScopes.orEmpty().joinToString(", ")
        val notesGranted = exception.grantedScopes.orEmpty().any { scope ->
            scope.contains("Notes.Read", ignoreCase = true)
        }
        return if (notesGranted) {
            SCOPE_PARTIAL_MESSAGE
        } else {
            buildString {
                append(SCOPE_DECLINED_MESSAGE)
                if (declined.isNotBlank()) append(" Declined: ").append(declined).append('.')
                if (granted.isNotBlank()) append(" Granted: ").append(granted).append('.')
            }
        }
    }

    private sealed interface TokenAttempt {
        data class Success(val auth: IAuthenticationResult) : TokenAttempt
        data object Cancelled : TokenAttempt
        data class Failed(val message: String) : TokenAttempt
    }

    companion object {
        private const val MSAL_CONFIG_FILE = "msal_onenote_config.json"
        private const val DEFAULT_TOKEN_TTL_MS = 3_600_000L
        private const val TOKEN_EXPIRY_SKEW_MS = 120_000L
        private const val DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common"
        private const val ACCOUNT_MISMATCH =
            "The signed in account does not match with the provided account."
        private const val SCOPE_DECLINED_MESSAGE =
            "Microsoft did not grant OneNote read access. In Entra → App registration → " +
                "API permissions, add Microsoft Graph delegated Notes.Read (and User.Read), " +
                "grant consent if required, then tap Connect OneNote again."
        private const val SCOPE_PARTIAL_MESSAGE =
            "Microsoft granted some permissions but UNFYND could not finish Connect. " +
                "Tap Connect OneNote once more."

        /**
         * Explicit Graph scopes only. Do not list `offline_access` / `openid` / `profile` —
         * MSAL adds those by default; requesting them again often causes DeclinedScope.
         */
        val SCOPES = arrayOf(
            "User.Read",
            "Notes.Read",
        )
    }
}
