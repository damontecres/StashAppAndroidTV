// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    kotlin("kapt") version "1.9.24" apply false
}
buildscript {
    repositories {
        google()
    }
    dependencies {
        val navVersion = "2.8.0-beta06"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
    }
}
