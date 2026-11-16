plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.paulo.filtrochamadas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.paulo.filtrochamadas"
        minSdk = 29          // ROLE_CALL_SCREENING exige Android 10+
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Sem dependências externas de propósito: só APIs do framework Android.
