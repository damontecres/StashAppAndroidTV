// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    kotlin("kapt") version "2.0.0" apply false
    alias(libs.plugins.compose.compiler) apply false
}
buildscript {
    repositories {
        google()
    }
    dependencies {
        val navVersion = "2.8.3"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
    }
}
