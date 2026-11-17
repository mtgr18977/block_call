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
        versionCode = 2
        versionName = "1.1"
    }

    // Chave de debug fixa no repositório: todo build sai com a mesma assinatura,
    // então o APK novo instala por cima do antigo sem perder permissões nem a função de triagem.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
