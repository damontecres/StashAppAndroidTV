plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.apollographql.apollo3") version "3.8.2"
}

android {
    namespace = "com.github.damontecres.stashapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.damontecres.stashapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

apollo {
    service("app") {
        packageName.set("com.github.damontecres.stashapp.api")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.leanback:leanback:1.1.0-rc02")
    implementation("androidx.leanback:leanback-preference:1.1.0-rc01")
    implementation("com.github.bumptech.glide:glide:4.11.0")

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")


    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")
    implementation("androidx.preference:preference:1.2.1")
}