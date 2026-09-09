plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.impad.pro"
    compileSdk = 36

    defaultConfig {
        applicationId = namespace
        minSdk = 27
        targetSdk = 36
        versionCode = 14
        versionName = "1.1.3"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.dexkit)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)
    compileOnly(libs.libxposed.api)
    implementation(libs.dexmaker)
    implementation(libs.androidx.annotation)
}