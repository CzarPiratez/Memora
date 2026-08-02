plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.room)
    alias(libs.plugins.legacyKapt)
    alias(libs.plugins.hilt)
}

// Public Microsoft client identifiers only (N2). Prefer local.properties;
// empty keeps Notes UI in registration-required honesty mode.
fun readMemoraLocalProperty(key: String): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return ""
    val prefix = "$key="
    return file.readLines()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && !it.startsWith("#") && it.startsWith(prefix) }
        ?.substringAfter("=", missingDelimiterValue = "")
        ?.trim()
        .orEmpty()
}
val oneNoteClientId = readMemoraLocalProperty("memora.onenote.clientId")
val oneNoteSignatureHash = readMemoraLocalProperty("memora.onenote.signatureHash")

android {
    namespace = "com.memora.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.memora.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ONENOTE_CLIENT_ID", "\"${oneNoteClientId.replace("\"", "\\\"")}\"")
        buildConfigField(
            "String",
            "ONENOTE_SIGNATURE_HASH",
            "\"${oneNoteSignatureHash.replace("\"", "\\\"")}\"",
        )
        // BrowserTabActivity path must use the raw signature hash (not URL-encoded).
        manifestPlaceholders["onenoteSignatureHash"] =
            oneNoteSignatureHash.ifBlank { "UNCONFIGURED" }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.pdfbox.android)
    // Explicitly replace PDFBox-Android's vulnerable Bouncy Castle 1.72 transitives
    // with the pinned versions reviewed in docs/dependency-review/.
    implementation(libs.bouncycastle.provider)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.bouncycastle.util)
    kapt(libs.room.compiler)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    // Production classpath for future PersistenceModule switch (Slice 1).
    // PersistenceModule still opens plaintext memora.db until the conversion gate.
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.exifinterface)
    implementation(libs.mlkit.text.recognition)
    // ADR-031: on-device text embeddings (model downloaded to private storage, not APK).
    implementation(libs.mediapipe.tasks.text)
    // Notes N2b: Microsoft identity for read-only OneNote connector (ADR-003).
    // Reviewed in docs/dependency-review/msal-android-8.4.1-review.md.
    implementation(libs.msal.android)
    // Explicit for OneNote Graph JSON parse (also a MSAL transitive; pin for unit tests).
    implementation(libs.gson)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

hilt {
    enableAggregatingTask = true
}
