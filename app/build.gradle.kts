import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.apollographql.apollo3") version "3.8.2"
}

fun getAppVersion(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "describe", "--tags", "--long")
        standardOutput = stdout
    }
    return stdout.toString().trim().removePrefix("v")
}

android {
    namespace = "com.github.damontecres.stashapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.github.damontecres.stashapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 2
        versionName = getAppVersion()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }

        applicationVariants.all {
            val variant = this
            variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { output ->
                    val outputFileName =
                        "StashAppAndroidTV-${variant.baseName}-${variant.versionName}-${variant.versionCode}.apk"
                    output.outputFileName = outputFileName
                }
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
        schemaFiles.setFrom(fileTree("../stash-server/graphql/schema/").filter { it.extension == "graphql" }.files.map { it.path })
        generateOptionalOperationVariables.set(false)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.leanback:leanback:1.1.0-rc02")
    implementation("androidx.leanback:leanback-preference:1.1.0-rc01")
    implementation("androidx.leanback:leanback-paging:1.1.0-alpha11")
    implementation("com.github.bumptech.glide:glide:4.11.0")

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
}