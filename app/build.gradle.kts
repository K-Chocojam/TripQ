
import java.util.Properties

// local.properties에서 Pleos 인증 정보 읽기
val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")

    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

val pleosClientId =
    localProperties.getProperty("PLEOS_CLIENT_ID", "")

val pleosClientSecret =
    localProperties.getProperty("PLEOS_CLIENT_SECRET", "")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tripq.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tripq.app"

        minSdk = 28
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // Pleos 인증 정보
        buildConfigField(
            "String",
            "PLEOS_CLIENT_ID",
            "\"$pleosClientId\""
        )

        buildConfigField(
            "String",
            "PLEOS_CLIENT_SECRET",
            "\"$pleosClientSecret\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
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

    testImplementation(libs.junit)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )
    androidTestImplementation(
        libs.androidx.espresso.core
    )
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    // Pleos FusedLocation SDK
    implementation("ai.pleos.playground:FusedLocation:1.1.3")

    // Pleos TextToSpeech SDK
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")

    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}
