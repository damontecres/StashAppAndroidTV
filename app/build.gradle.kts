import com.android.build.gradle.internal.cxx.io.writeTextIfDifferent
import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Properties

val isCI = if (System.getenv("CI") != null) System.getenv("CI").toBoolean() else false
val shouldSign = isCI && System.getenv("KEY_ALIAS") != null

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.apollo)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.compose.compiler)
}

fun getVersionCode(): Int {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "tag", "--list", "v*")
        standardOutput = stdout
    }
    return stdout
        .toString()
        .trim()
        .lines()
        .size
}

fun getAppVersion(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "describe", "--tags", "--long", "--match=v*")
        standardOutput = stdout
    }
    return stdout
        .toString()
        .trim()
        .removePrefix("v")
        .ifBlank { "0.0.0" }
}

android {
    namespace = "com.github.damontecres.stashapp"
    compileSdk = 35

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "$buildDir/generated/res/server")
        }
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.github.damontecres.stashapp"
        minSdk = 23
        targetSdk = 35
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
            } else {
                val properties = Properties()
                properties.load(project.rootProject.file("local.properties").inputStream())
                val signingConfigName = properties["release.signing.config"]?.toString()
                if (signingConfigName != null) {
                    signingConfig = signingConfigs.getByName(signingConfigName)
                }
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    lint {
        disable.add("MissingTranslation")
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

tasks.register("createGraphqlSchema") {
    group = "build"
    description = "Concats all of the server graphql scehem files"

    doFirst {
        File("$projectDir/src/main/graphql/schema.graphqls").writeTextIfDifferent(
            "# Auto-generated do not edit\n\n" +
                fileTree("../stash-server/graphql/schema/")
                    .filter { it.extension == "graphql" }
                    .files
                    .sorted()
                    .joinToString("\n") { it.readText() },
        )
    }
}

tasks.register("cleanGraphqlSchema") {
    group = "clean"
    description = "Deletes generated graphql schema"

    doFirst {
        File("$projectDir/src/main/graphql/schema.graphqls").delete()
    }
}

apollo {
    service("app") {
        packageName.set("com.github.damontecres.stashapp.api")
        schemaFile = File("$projectDir/src/main/graphql/schema.graphqls")
        generateOptionalOperationVariables.set(false)
        outputDirConnection {
            // Fixes where classes aren't detected in unit tests
            // See: https://community.apollographql.com/t/android-warning-duplicate-content-roots-detected-after-just-adding-apollo3-kotlin-client/4529/6
            connectToKotlinSourceSet("main")
        }
        plugin(project(":apollo-compiler"))
    }
}

tasks.named("generateAppApolloSources") {
    dependsOn("createGraphqlSchema")
}

tasks.register<com.github.damontecres.buildsrc.ParseStashStrings>("generateStrings") {
    sourceDirectory = File("$projectDir/../stash-server/ui/v2.5/src/locales")
    outputDirectory = File("$buildDir/generated/res/server")
}

// tasks.preBuild.dependsOn("generateStrings")
tasks.preBuild.dependsOn("generateStrings")
tasks.clean.dependsOn("cleanGraphqlSchema")

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.leanback.preference)
    implementation(libs.androidx.leanback.tab)

    implementation(libs.glide)
    implementation(libs.glide.okhttp3.integration)
    ksp(libs.glide.ksp)

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
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.previewseekbar)
    implementation(libs.previewseekbar.media3)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.zoomlayout)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.markwon.core)
    implementation(libs.parcelable.core)

    implementation(libs.acra.http)
    implementation(libs.acra.dialog)
    implementation(libs.acra.limiter)
    compileOnly(libs.auto.service.annotations)
    ksp(libs.auto.service)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.glide.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.material3.android)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.ui.viewbinding)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.android.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidsvg.aar)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.network.cachecontrol)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)
    implementation(libs.navigation.reimagined)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.truth)
}
