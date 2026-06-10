import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// AGP 9.2.1's kmp-library plugin pulls Kotlin stdlib 2.3.21; align it to the 2.2.10 compiler (klib match).
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion("2.2.20")
        }
    }
}

kotlin {
    // :composeApp is now a Compose Multiplatform LIBRARY (android+desktop+wasm); the Android *application*
    // lives in the thin :androidApp module. This avoids com.android.application + KMP (which needs the
    // AGP9 bypass that breaks non-JVM targets).
    androidLibrary {
        // Distinct from the app's namespace (:androidApp owns pro.jayeshseth.devcontrols). This library is
        // code-only (its res/manifest moved to :androidApp, since kmp-library doesn't export resources).
        namespace = "pro.jayeshseth.devcontrols.shared"
        compileSdk = 36
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
    applyDefaultHierarchyTemplate()

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

compose.desktop {
    application {
        mainClass = "pro.jayeshseth.devcontrols.MainKt"
    }
}
