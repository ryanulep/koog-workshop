plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.koog.agents)
    implementation(libs.koog.agents.additions)
    implementation(libs.koog.spring.ai.starter.vectorStore)
    implementation(libs.spring.ai.starter.vectorStore.pgvector)
    implementation(libs.spring.ai.openai)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
}