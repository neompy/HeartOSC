plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "red.kitsu.heartosc"
    compileSdk = 34

    defaultConfig {
        applicationId = "red.kitsu.heartosc"
        minSdk = 30
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    // ADD THESE TWO BLOCKS:
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("com.google.android.gms:play-services-wearable:18.0.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.wear:wear:1.3.0")
    
    // Jetpack Health Services for Wear OS 3+ Heart Rate
    implementation("androidx.health:health-services-client:1.0.0-beta03")
    implementation("com.google.guava:guava:31.1-android")
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.6.4")
}
