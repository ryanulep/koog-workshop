import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    kotlin("plugin.spring")
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xskip-prerelease-check",
            "-Xexplicit-backing-fields"
        )
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("ai.koog:koog-spring-ai-starter-model-chat:0.8.0")
    implementation("ai.koog:koog-spring-ai-starter-chat-memory:0.8.0")
    implementation("ai.koog:agents-features-persistence-jdbc:0.8.0")
    implementation("ai.koog:agents-features-opentelemetry:0.8.0")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.kotlinx.serialization.json)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc:2.0.0-M5")
    implementation("org.springframework.ai:spring-ai-starter-model-openai:2.0.0-M5")

    implementation("org.xerial:sqlite-jdbc")
    implementation("org.jetbrains.exposed:exposed-spring-boot4-starter:1.2.0")

    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.sqlite.jdbc)
    implementation(libs.koog.agents)
    implementation(libs.koog.memory)
    implementation(libs.markdown.renderer)
    implementation(libs.logback.classic)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

repositories {
    mavenCentral()
}