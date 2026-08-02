plugins {
    id("com.android.application")
}

android {
    namespace = "com.smartkey.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartkey.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Secure local storage for the user's own API key
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Networking to call the Claude API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
