plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "it.studiosmus.carte"
    compileSdk = 35
    defaultConfig {
        applicationId = "it.studiosmus.carte"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.1.4"
    }
    // CI test builds use a stable, deliberately non-release signing key.
    // Local developer builds retain the normal Android debug key if absent.
    signingConfigs {
        getByName("debug") {
            val ciKey = rootProject.file("app/ci-debug.keystore")
            if (ciKey.isFile) {
                storeFile = ciKey
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
                storeType = "jks"
            }
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~:!CARTE-HD-assets.zip:!README.md"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
