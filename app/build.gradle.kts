plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hydradroid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hydradroid"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.1"

        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "HYDRA_API_URL", "\"https://hydra-api-us-east-1.losbroxas.org\"")
        buildConfigField("String", "HYDRA_AUTH_URL", "\"https://auth.hydra.losbroxas.org\"")
    }

    buildTypes {
        release {
            // R8 + shrink: меньше dex/recursos → быстрее холодный старт и установка.
            // Debug остаётся без минификации ради скорости итерации.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Тестовые релизные сборки подписываем debug-ключом (CI/sideload).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)
    implementation(libs.datastore)
    implementation(libs.security.crypto)
    implementation(libs.browser)
    // Торрент-движок: libtorrent через jlibtorrent (порт python-rpc/libtorrent :5881)
    implementation(libs.jlibtorrent)
    implementation(libs.jlibtorrent.android.arm)
    implementation(libs.jlibtorrent.android.arm64)
    implementation(libs.jlibtorrent.android.x86)
    implementation(libs.jlibtorrent.android.x8664)
    // Распаковщик: 7z/zip/tar + rar (порт людосави/7z-части оригинала)
    implementation(libs.commons.compress)
    implementation(libs.junrar)
    implementation(libs.documentfile) // SAF-выбор папки загрузок
}
