repositories {
    mavenCentral()
}

plugins {
//    `kotlin-dsl`
    kotlin("jvm") version "1.9.23"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
