package com.memora.app.data.notes

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N2a gate: packaged app resources must not contain client secrets or fixture
 * refresh/access tokens for the OneNote path.
 */
class NotesProviderSecretAbsenceTest {

    @Test
    fun mainResourcesDoNotEmbedClientSecretsOrTokenFixtures() {
        val roots = listOf(
            File("src/main/res"),
            File("src/main/assets"),
            File("src/main/java/com/memora/app"),
        ).filter { it.exists() }
        assertTrue("Expected main source roots to exist for scanning.", roots.isNotEmpty())

        val forbidden = listOf(
            "client_secret",
            "clientSecret",
            "refresh_token",
            "\"access_token\"",
            "Bearer eyJ",
        )
        val hits = mutableListOf<String>()
        roots.forEach { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension in setOf("xml", "json", "txt", "kt", "properties") }
                .forEach { file ->
                    val text = file.readText()
                    forbidden.forEach { needle ->
                        if (text.contains(needle)) {
                            hits += "${file.path}: contains $needle"
                        }
                    }
                }
        }
        assertFalse(
            "Found forbidden secret/token fixtures:\n${hits.joinToString("\n")}",
            hits.isNotEmpty(),
        )
    }
}
