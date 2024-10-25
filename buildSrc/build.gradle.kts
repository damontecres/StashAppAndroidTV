repositories {
    mavenCentral()
}

plugins {
    alias(libs.plugins.kotlin.jvm)
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
    implementation(libs.kotlinx.serialization.json)
}
