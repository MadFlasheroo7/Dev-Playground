plugins {
    alias(libs.plugins.android.application)
}

android {
    // Same package as the app classes (in :composeApp) so the merged manifest's relative names
    // (.MainActivity, .widget.*, .adb.*) resolve correctly, and so res references (@style/Theme.DevControls,
    // @mipmap/ic_launcher, @xml/*) live in this app module's R = pro.jayeshseth.devcontrols.
    namespace = "pro.jayeshseth.devcontrols"
    compileSdk = 37

    defaultConfig {
        applicationId = "pro.jayeshseth.devcontrols"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // youtubedl-android (pulled transitively from :media) reads its bundled Python from nativeLibraryDir,
    // so native libs must be extracted to disk on install.
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
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":composeApp"))
}
