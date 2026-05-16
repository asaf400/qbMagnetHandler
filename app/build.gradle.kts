plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.example.magnetforwarder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.magnetforwarder"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.coroutinesAndroid)

    implementation(libs.androidxWorkRuntimeKtx)
    implementation(libs.androidxSecurityCrypto)

    implementation(libs.okhttp)
    implementation(libs.okhttpUrlConnection)
    implementation(libs.material)

    implementation(platform(libs.composeBomLib))
    implementation(libs.composeUi)
    implementation(libs.composeUiToolingPreview)
    implementation(libs.composeMaterial3)

    debugImplementation(libs.composeUiTooling)
}

