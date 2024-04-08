import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.ByteArrayOutputStream
import java.util.Base64

val isCI = if (System.getenv("CI") != null) System.getenv("CI").toBoolean() else false
val shouldSign = isCI && System.getenv("KEY_ALIAS") != null

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.apollographql.apollo3") version "3.8.2"
    id("kotlin-kapt")
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

    defaultConfig {
        applicationId = "com.github.damontecres.stashapp"
        minSdk = 23
        targetSdk = 34
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
    }
}

tasks.create("generateStrings", Exec::class.java) {
    if (gradle.startParameter.logLevel == LogLevel.DEBUG || gradle.startParameter.logLevel == LogLevel.INFO) {
        commandLine("python", "convert_strings.py", "--debug")
    } else {
        commandLine("python", "convert_strings.py")
    }
}

tasks.preBuild.dependsOn("generateStrings")

val mediaVersion = "1.2.1"
val glideVersion = "4.16.0"

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.leanback:leanback:1.1.0-rc02")
    implementation("androidx.leanback:leanback-preference:1.1.0-rc01")
    implementation("androidx.leanback:leanback-paging:1.1.0-alpha11")
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    implementation("com.github.bumptech.glide:okhttp3-integration:$glideVersion")
    implementation("androidx.leanback:leanback-tab:1.1.0-beta01")
    kapt("com.github.bumptech.glide:compiler:$glideVersion")
    implementation("com.caverock:androidsvg-aar:1.4")

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    implementation("com.apollographql.apollo3:apollo-runtime:3.8.2")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.media3:media3-exoplayer:$mediaVersion")
    implementation("androidx.media3:media3-ui:$mediaVersion")
    implementation("androidx.media3:media3-exoplayer-dash:$mediaVersion")
    implementation("androidx.media3:media3-exoplayer-hls:$mediaVersion")
    implementation("com.github.rubensousa:previewseekbar:3.1.1")
    implementation("com.github.rubensousa:previewseekbar-media3:1.1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("com.otaliastudios:zoomlayout:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.9.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}
