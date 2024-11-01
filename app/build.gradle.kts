import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.ByteArrayOutputStream
import java.util.Base64

val isCI = if (System.getenv("CI") != null) System.getenv("CI").toBoolean() else false
val shouldSign = isCI && System.getenv("KEY_ALIAS") != null

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.apollo)
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.compose.compiler)
}

fun getVersionCode(): Int {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "tag", "--list", "v*")
        standardOutput = stdout
    }
    return stdout.toString().trim().lines().size
}

fun getAppVersion(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "describe", "--tags", "--long", "--match=v*")
        standardOutput = stdout
    }
    return stdout.toString().trim().removePrefix("v")
}

android {
    namespace = "com.github.damontecres.stashapp"
    compileSdk = 34

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "$buildDir/generated/res/server")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    defaultConfig {
        applicationId = "com.github.damontecres.stashapp"
        minSdk = 23
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = getVersionCode()
        versionName = getAppVersion()
        vectorDrawables.useSupportLibrary = true
    }
    signingConfigs {
        if (shouldSign) {
            create("ci") {
                file("ci.keystore").writeBytes(
                    Base64.getDecoder().decode(System.getenv("SIGNING_KEY")),
                )
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                storePassword = System.getenv("KEY_STORE_PASSWORD")
                storeFile = file("ci.keystore")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (shouldSign) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        debug {
            if (shouldSign) {
                signingConfig = signingConfigs.getByName("ci")
            }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    lint {
        disable.add("MissingTranslation")
    }
}

apollo {
    service("app") {
        packageName.set("com.github.damontecres.stashapp.api")
        schemaFiles.setFrom(fileTree("../stash-server/graphql/schema/").filter { it.extension == "graphql" }.files.map { it.path })
        generateOptionalOperationVariables.set(false)
        outputDirConnection {
            // Fixes where classes aren't detected in unit tests
            // See: https://community.apollographql.com/t/android-warning-duplicate-content-roots-detected-after-just-adding-apollo3-kotlin-client/4529/6
            connectToKotlinSourceSet("main")
        }
        plugin(project(":apollo-compiler"))
    }
}

tasks.register<com.github.damontecres.buildsrc.ParseStashStrings>("generateStrings") {
    sourceDirectory = File("$projectDir/../stash-server/ui/v2.5/src/locales")
    outputDirectory = File("$buildDir/generated/res/server")
}

// tasks.preBuild.dependsOn("generateStrings")
tasks.preBuild.dependsOn("generateStrings")

val navVersion = "2.8.3"

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.leanback.preference)
    implementation(libs.androidx.leanback.paging)
    implementation(libs.androidx.leanback.tab)

    implementation(libs.glide)
    implementation(libs.glide.okhttp3.integration)
    ksp(libs.glide.ksp)
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")

    implementation(libs.android.material)

    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.androidsvg.aar)
    implementation(kotlin("reflect"))

    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.apollo.runtime)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.previewseekbar)
    implementation(libs.previewseekbar.media3)
    implementation(libs.androidx.constraintlayout)

    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.navigation:navigation-runtime-ktx:$navVersion")
    implementation("androidx.compose.runtime:runtime-android:1.6.8")
    implementation("androidx.navigation:navigation-compose:$navVersion")
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.material3:material3-android:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.tv:tv-foundation:1.0.0-alpha11")
    implementation("androidx.tv:tv-material:1.0.0-rc01")
    implementation("androidx.compose.ui:ui-viewbinding:1.7.0-beta06")
    implementation("androidx.paging:paging-runtime-ktx:3.3.1")
    implementation("androidx.paging:paging-compose:3.3.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.caverock:androidsvg-aar:1.4")

    implementation(libs.zoomlayout)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.core)
    implementation(libs.parcelable.core)

    implementation(libs.acra.http)
    implementation(libs.acra.dialog)
    implementation(libs.acra.limiter)
    compileOnly(libs.auto.service.annotations)
    ksp(libs.auto.service)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.truth)
}
