import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dhp.thl.tpl.vkn.spotify"
    compileSdk = 34

    defaultConfig {
        applicationId = "dhp.thl.tpl.vkn.spotify"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            val keystoreEnv = System.getenv("KEYSTORE_PATH")
            val keystoreFile = when {
                !keystoreEnv.isNullOrEmpty() && file(keystoreEnv).exists() -> file(keystoreEnv)
                rootProject.file("my-release-key.jks").exists() -> rootProject.file("my-release-key.jks")
                file("my-release-key.jks").exists() -> file("my-release-key.jks")
                File(System.getProperty("user.home"), "my-release-key.jks").exists() -> File(System.getProperty("user.home"), "my-release-key.jks")
                else -> null
            }

            val storePass = System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("KEY_STORE_PASSWORD")
            val alias = System.getenv("KEY_ALIAS") ?: System.getenv("KEYSTORE_ALIAS")
            val keyPass = System.getenv("KEY_PASSWORD") ?: storePass

            if (keystoreFile != null && !alias.isNullOrEmpty() && !storePass.isNullOrEmpty()) {
                storeFile = keystoreFile
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
}
