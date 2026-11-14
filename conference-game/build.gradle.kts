import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "pro.jayeshseth.bugrush"
        compileSdk = 37
        minSdk = 30
    }

    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Shader Engine Integration
            implementation(project(":shader-engine"))

            // Kubriko Game Engine and Plugins
            implementation("io.github.pandulapeter.kubriko:engine:0.8.1")
            implementation("io.github.pandulapeter.kubriko:plugin-keyboard-input:0.8.1")
            implementation("io.github.pandulapeter.kubriko:plugin-gamepad-input:0.8.1")
            implementation("io.github.pandulapeter.kubriko:plugin-pointer-input:0.8.1")
            implementation("io.github.pandulapeter.kubriko:plugin-audio-playback:0.8.1")
            implementation("io.github.pandulapeter.kubriko:plugin-sprites:0.8.1")
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.macos_arm64)
                implementation(compose.desktop.macos_x64)
                implementation(compose.desktop.windows_x64)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.compose.ui.tooling)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "pro.jayeshseth.bugrush.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "kotlin-bug-rush"
            packageVersion = "1.0.0"
            description = "Kotlin Bug Rush - Tech Conference Arcade"
            copyright = "© 2026 pro.jayeshseth"
            vendor = "pro.jayeshseth"

            linux {
                packageName = "kotlin-bug-rush"
                menuGroup = "Game"
            }
        }
    }
}
