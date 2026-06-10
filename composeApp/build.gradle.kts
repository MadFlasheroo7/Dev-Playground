import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    // Web (Wasm) DEFERRED — blocked structurally, not by version: with the AGP 9 bypass
    // (android.builtInKotlin=false/newDsl=false, required for com.android.application + KMP),
    // KMP does not wire Compose deps onto the wasm classpath (androidx.compose.* unresolved).
    // Confirmed across CMP 1.11.1 / 1.12.0-alpha01 and Kotlin 2.2.10 / 2.2.20. Desktop+Android
    // (JVM) resolve fine. Real fix = drop the bypass via a 2-module split (com.android.application
    // app module + com.android.kotlin.multiplatform.library shared module). Sources kept.
    // @OptIn(ExperimentalWasmDsl::class)
    // wasmJs {
    //     browser { commonWebpackConfig { outputFileName = "devcontrols.js" } }
    //     binaries.executable()
    // }

    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project(":media"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.graphics.shapes)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.androidx.camera.video)
            implementation(libs.androidx.camera.extensions)
            implementation(libs.mlkit.doc.scanner)
            implementation(libs.zxing.core)
            implementation(libs.mediapipe.tasks.vision)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.libadb.android)
            implementation(libs.sun.security.android)
            implementation(libs.conscrypt.android)
        }
    }
}

android {
    namespace = "pro.jayeshseth.devcontrols"
    compileSdk = 36

    defaultConfig {
        applicationId = "pro.jayeshseth.devcontrols"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].assets.srcDirs("src/androidMain/assets")

    // youtubedl-android reads its bundled Python from nativeLibraryDir at runtime, so the native libs
    // must be extracted to disk on install (modern AGP defaults to not extracting them).
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

compose.desktop {
    application {
        mainClass = "pro.jayeshseth.devcontrols.MainKt"
    }
}
