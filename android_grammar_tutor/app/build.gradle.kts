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
        versionCode = 3
        versionName = "2.1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
