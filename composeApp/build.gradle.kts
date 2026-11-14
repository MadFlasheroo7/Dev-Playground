import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // :composeApp is now a Compose Multiplatform LIBRARY (android+desktop+wasm); the Android *application*
    // lives in the thin :androidApp module. This avoids com.android.application + KMP (which needs the
    // AGP9 bypass that breaks non-JVM targets).
    androidLibrary {
        // Distinct from the app's namespace (:androidApp owns pro.jayeshseth.devcontrols). This library is
        // code-only (its res/manifest moved to :androidApp, since kmp-library doesn't export resources).
        namespace = "pro.jayeshseth.devcontrols.shared"
        compileSdk = 37
        minSdk = 30
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser { commonWebpackConfig { outputFileName = "devcontrols.js" } }
        binaries.executable()
    }
    js(IR) {
        browser { commonWebpackConfig { outputFileName = "devcontrols.js" } }
        binaries.executable()
    }
    // iOS — declared so iosMain is authored & ready; the iOS app/framework is built on macOS (Xcode),
    // so these targets' compile/link tasks don't run on this Linux host (others are unaffected).
    iosArm64()
    iosSimulatorArm64()
    applyDefaultHierarchyTemplate()

    sourceSets {
        // webMain (js + wasmJs) comes from the default hierarchy template; the web entry point,
        // YouTube-IFrame MediaController and capability stubs live there (written once).
        val webMain by getting
        webMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.browser)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.datetime)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime) // multiplatform date/time (replaces java.util.Calendar etc. for wasm)
            implementation(project.dependencies.platform(libs.kotlincrypto.hash.bom))
            implementation(libs.kotlincrypto.md)
            implementation(libs.kotlincrypto.sha1)
            implementation(libs.kotlincrypto.sha2)
            implementation(project(":media"))
            implementation(project(":shader-engine"))
            implementation(project(":bottomsheet"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.datetime)
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
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.libadb.android)
            implementation(libs.sun.security.android)
            implementation(libs.conscrypt.android)
            implementation(project(":audio-studio"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "pro.jayeshseth.devcontrols.MainKt"
    }
}
