import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

// SPIKE diagnostic: force Kotlin stdlib to match the 2.2.10 compiler (AGP9 kmp-library pulls 2.3.21).
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
            useVersion("2.2.10")
        }
    }
}

kotlin {
    // com.android.kotlin.multiplatform.library: the Android target is declared here (no top-level android{}).
    androidLibrary {
        namespace = "pro.jayeshseth.media"
        compileSdk = 36
        minSdk = 30
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    // iOS targets added later (Media 5).

    // Keep the default hierarchy (commonMain → every target incl. wasm) AND add a jvmShared intermediate
    // for the JVM targets. Without applyDefaultHierarchyTemplate(), the manual dependsOn edges below would
    // disable the template and orphan wasmJs from commonMain (→ stdlib unresolved on wasm).
    applyDefaultHierarchyTemplate()

    sourceSets {
        // Intermediate source set shared by the two JVM targets, where the NewPipeExtractor
        // (JVM-only) actual lives once — Android + Desktop both consume it.
        val commonMain by getting
        val jvmShared by creating { dependsOn(commonMain) }
        val androidMain by getting { dependsOn(jvmShared) }
        val desktopMain by getting { dependsOn(jvmShared) }

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }
        jvmShared.dependencies {
            implementation(libs.newpipeextractor)
            implementation(libs.okhttp)
        }
        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            // so ExoPlayer can also play the adaptive HLS/DASH manifests when bestPlayable() falls back to them
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.exoplayer.dash)
            // yt-dlp on Android (bundles Python) — resolves uncapped YouTube stream URLs (NewPipe's are PoToken-capped)
            implementation(libs.youtubedl.android)
        }
        desktopMain.dependencies {
            implementation(libs.vlcj)
        }
    }
}
