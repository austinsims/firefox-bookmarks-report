import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    application
}

group = "me.asims"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.spullara.mustache.java:compiler:0.9.7")
    implementation("com.google.guava:guava:18.0")
    implementation("com.google.guava:guava:30.1-jre")
    implementation("com.sparkjava:spark-kotlin:1.0.0-alpha")
    implementation("commons-cli:commons-cli:1.4")
    runtimeOnly("org.xerial:sqlite-jdbc:jar:3.34.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}