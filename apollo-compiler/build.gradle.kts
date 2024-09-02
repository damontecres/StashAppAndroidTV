plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.apollographql.apollo:apollo-compiler:4.0.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
}
