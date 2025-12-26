plugins {
    kotlin("jvm") version "2.1.20"
    id("java-library")
    `maven-publish`
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
allprojects {
    repositories {
        mavenCentral()
        google()
    }
    group = "org.example"
    version = "1.0"
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-network:3.1.3")
    implementation("io.ktor:ktor-network-tls:3.1.3")
    implementation("io.ktor:ktor-network-tls:3.1.3")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
