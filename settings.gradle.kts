pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // PREFER_SETTINGS (not FAIL_ON_PROJECT_REPOS) so the Kotlin/Wasm toolchain can add its Node.js
    // distribution repository (needed by webpack for the wasm browser bundle).
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Kotlin/Wasm downloads the Node.js runtime (for webpack) as org.nodejs:node from here.
        ivy {
            name = "Node.js Distributions"
            setUrl("https://nodejs.org/dist")
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy {
            name = "Yarn Distributions"
            setUrl("https://github.com/yarnpkg/yarn/releases/download")
            patternLayout { artifact("v[revision]/[artifact]-v[revision].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
        ivy {
            name = "Binaryen Distributions"
            setUrl("https://github.com/WebAssembly/binaryen/releases/download")
            patternLayout { artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
    }
}

rootProject.name = "DevControls"
include(":androidApp")   // thin com.android.application shell (manifest + applicationId)
include(":composeApp")   // shared Compose Multiplatform LIBRARY (android+desktop+wasm)
include(":media")
 