import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.FilterConfiguration
import com.android.build.gradle.internal.cxx.io.writeTextIfDifferent
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.google.protobuf.gradle.id
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64

val isCI = if (System.getenv("CI") != null) System.getenv("CI").toBoolean() else false
val shouldSign = isCI && System.getenv("KEY_ALIAS") != null
val extensionsRepoActive = project.hasProperty("WholphinExtensionsUsername")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.apollo)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.protobuf)
}

val gitTags =
    providers
        .exec { commandLine("git", "tag", "--list", "v*") }
        .standardOutput.asText
        .get()

val gitDescribe =
    providers
        .exec { commandLine("git", "describe", "--tags", "--long", "--match=v*") }
        .standardOutput.asText
        .getOrElse("v0.0.0")

kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
        jvmTarget = JvmTarget.JVM_21
        javaParameters = true
    }
}

configure<ApplicationExtension> {
    namespace = "com.github.damontecres.stashapp"
    compileSdk = 36

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
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = gitTags.trim().lines().size
        versionName = gitDescribe.trim().removePrefix("v").ifBlank { "0.0.0" }
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
            isDebuggable = false

            if (shouldSign) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            if (shouldSign) {
                signingConfig = signingConfigs.getByName("ci")
            }
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    lint {
        disable.add("MissingTranslation")
        disable.add("LocalContextGetResourceValueCall") // TODO
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs
            .map { it as com.android.build.api.variant.impl.VariantOutputImpl }
            .forEach { output ->
                val abi = output.getFilter(FilterConfiguration.FilterType.ABI)?.identifier
                val parts =
                    listOf(
                        "StashAppAndroidTV",
                        variant.flavorName,
                        variant.buildType,
                        output.versionName.get(),
                        output.versionCode.get().toString(),
                        abi,
                    ).filterNot { it.isNullOrBlank() }
                val outputFileName = parts.joinToString("-")
                output.outputFileName = "$outputFileName.apk"
            }
    }
}

tasks.register("createGraphqlSchema") {
    group = "build"
    description = "Concats all of the server graphql scehem files"

    doFirst {
        File("$projectDir/src/main/graphql/schema.graphqls").writeTextIfDifferent(
            "# Auto-generated do not edit\n\n" +
                File("$projectDir/src/main/graphql/client_schema.graphqls").readText() +
                "\n\n" +
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

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.protobuf.kotlin.lite.get().version}"
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("java") {
                    option("lite")
                }
            }
            it.builtins {
                id("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

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
    implementation(libs.compose.wheel.picker)
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

    implementation(libs.androidx.compose.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.material3.adaptive)
    implementation(libs.androidx.material3.window.size)
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
    implementation(libs.restring)
    implementation(libs.viewpump)
    implementation(libs.reword)
    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.multiplatform.markdown.renderer)
    implementation(libs.multiplatform.markdown.renderer.m3)

    implementation(libs.timber)
    implementation(libs.slf4j2.timber)
    if (extensionsRepoActive) {
        implementation(libs.wholphin.extensions.mpv)
        implementation(libs.wholphin.extensions.ffmpeg)
        implementation(libs.wholphin.extensions.av1)
    } else {
        logger.warn("Native extensions will not be included")
    }

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
