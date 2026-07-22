package com.memora.app.data.pdfbox.isolation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memora.app.data.pdfbox.PdfBoxPdfDocumentParser
import com.memora.app.data.pdfbox.PdfDocumentParseResult
import com.memora.app.data.pdfbox.SyntheticPdfFixtures
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the deterministic parser against a repository-owned PDF only after Android reports
 * that the emulator has no Internet-capable network. It never opens a user document, binds the
 * isolated service, accesses SAF, persists data, invokes AI, or makes a network request.
 */
@RunWith(AndroidJUnit4::class)
class PdfParserOfflineRuntimeIntegrationTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val testContext = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun initializePdfBoxRuntime() {
        PDFBoxResourceLoader.init(appContext)
    }

    @Test
    fun release_app_manifest_does_not_request_internet_permission() {
        val requestedPermissions = appContext.requestedPermissions()

        assertFalse(
            "Memora's release app must not request Internet access.",
            Manifest.permission.INTERNET in requestedPermissions,
        )
    }

    @Test
    fun parser_runs_only_when_the_emulator_reports_no_internet_capable_network() {
        assertReleaseAppHasNoInternetPermission()
        assertEmulatorIsOffline()

        val parsed = SyntheticPdfFixtures.twoPageSelectable().use { input ->
            PdfBoxPdfDocumentParser().parse(input)
        }
        val result = parsed as? PdfDocumentParseResult.Parsed
            ?: throw AssertionError("The repository-owned selectable-text fixture must parse.")

        assertEquals(2, result.pageCount)
        assertTrue(result.pages.sumOf { page -> page.text.length } > 0)
    }

    private fun assertReleaseAppHasNoInternetPermission() {
        assertFalse(
            "Memora's release app must not request Internet access.",
            Manifest.permission.INTERNET in appContext.requestedPermissions(),
        )
    }

    private fun assertEmulatorIsOffline() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.adoptShellPermissionIdentity(Manifest.permission.ACCESS_NETWORK_STATE)
        val hasInternetCapableNetwork = try {
            val connectivityManager = testContext.getSystemService(ConnectivityManager::class.java)
            val capabilities = connectivityManager.activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }
            capabilities?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } ?: false
        } finally {
            uiAutomation.dropShellPermissionIdentity()
        }

        assertFalse(
            "Offline test precondition not met. Disable Wi-Fi and mobile data for the Medium " +
                "Phone emulator, wait for its status bar to show no connection, then run this " +
                "class again. The parser was not invoked.",
            hasInternetCapableNetwork,
        )
    }

    @Suppress("DEPRECATION")
    private fun Context.requestedPermissions(): Set<String> =
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toSet()
            .orEmpty()
}
