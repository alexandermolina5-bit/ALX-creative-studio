plugins { id("com.android.application") }

android {
    namespace = "com.alx.creativestudio"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alx.creativestudio"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "3.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
