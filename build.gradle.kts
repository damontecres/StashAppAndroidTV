// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
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
