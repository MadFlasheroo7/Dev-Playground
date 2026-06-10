import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    // iOS + Wasm targets are added later (Media 5) — they use the Piped REST actuals and can't
    // build on this Linux host anyway.

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

android {
    namespace = "pro.jayeshseth.media"
    compileSdk = 36
    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
