plugins {
    id("com.android.application")
}

android {
    namespace = "com.james.gramaticaconversacional"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.james.gramaticaconversacional"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
