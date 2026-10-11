plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.impad.pro"
    compileSdk = 36

    defaultConfig {
        // applicationId 必须与 LSPosed 模块仓库（Xposed-Modules-Repo）的仓库名一致。
        // 官方 bot 只自动放行两类包名：以 io.github.<用户名>. 开头，或包名反解的根域名挂有
        // TXT 记录 lsposed-modules-repo-verification=<用户名>。com.impad.pro 反解根域为 impad.com
        // （非本项目所有），故改用 io.github.symonchu.*。namespace 保持不变：
        // 入口类 com.impad.pro.HookEntrance、proguard keep 规则、Zygisk JNI 类名都依赖它。
        applicationId = "io.github.symonchu.impadpro"
        minSdk = 27
        targetSdk = 36
        versionCode = 16
        versionName = "1.2.0"
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