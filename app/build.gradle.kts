import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.ByteArrayOutputStream
import java.util.Base64

val isCI = if (System.getenv("CI") != null) System.getenv("CI").toBoolean() else false
val shouldSign = isCI && System.getenv("KEY_ALIAS") != null

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.apollographql.apollo") version "4.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    kotlin("kapt")
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

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

// tasks.preBuild.dependsOn("generateStrings")
tasks.preBuild.dependsOn("generateStrings")

val mediaVersion = "1.4.1"
val glideVersion = "4.16.0"
val acraVersion = "5.11.3"
val navVersion = "2.8.3"
val roomVersion = "2.6.1"

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.leanback:leanback:1.1.0-rc02")
    implementation("androidx.leanback:leanback-preference:1.1.0-rc01")
    implementation("androidx.leanback:leanback-paging:1.1.0-alpha11")
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.github.bumptech.glide:okhttp3-integration:$glideVersion")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")
    implementation("androidx.leanback:leanback-tab:1.1.0-beta01")

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
    ksp("com.github.bumptech.glide:ksp:$glideVersion")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation(kotlin("reflect"))

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-process:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.apollographql.apollo:apollo-runtime:4.0.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.media3:media3-exoplayer:$mediaVersion")
    implementation("androidx.media3:media3-ui:$mediaVersion")
    implementation("androidx.media3:media3-exoplayer-dash:$mediaVersion")
    implementation("androidx.media3:media3-exoplayer-hls:$mediaVersion")
    implementation("androidx.media3:media3-datasource-okhttp:$mediaVersion")
    implementation("androidx.media3:media3-effect:$mediaVersion")
    implementation("androidx.media3:media3-transformer:$mediaVersion")
    implementation("com.github.rubensousa:previewseekbar:3.1.1")
    implementation("com.github.rubensousa:previewseekbar-media3:1.1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.otaliastudios:zoomlayout:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("com.chrynan.parcelable:parcelable-core:0.9.0")

    implementation("ch.acra:acra-http:$acraVersion")
    implementation("ch.acra:acra-dialog:$acraVersion")
    implementation("ch.acra:acra-limiter:$acraVersion")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    ksp("com.google.auto.service:auto-service:1.1.1")

    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    implementation("androidx.test.ext:junit-ktx:1.2.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.9.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.ext:truth:1.6.0")
}
