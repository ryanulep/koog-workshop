import java.util.*

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

fun registerRunTask(
    name: String,
    mainClassName: String,
) {
    tasks.register<JavaExec>(name) {
        group = "application"
        description = "Runs a main class after loading env.properties"

        val mainClassName = mainClassName

        doFirst {
            standardInput = System.`in`
            standardOutput = System.out

            val envFile = project.file("env.properties")
            if (envFile.exists()) {
                val props = Properties().apply {
                    envFile.inputStream().use(::load)
                }

                props.forEach { (key, value) ->
                    environment(key.toString(), value.toString())
                }
            }

            mainClass.set(mainClassName)
        }

        classpath = sourceSets["main"].runtimeClasspath
    }
}

registerRunTask("runMemoryExample", "org.example.advanced.MemoryExampleKt")
registerRunTask("runAgentContextExample", "org.example.advanced.AgentContextExampleKt")
registerRunTask("runStructuredOutputExample", "org.example.advanced.StructuredOutputExampleKt")
registerRunTask("runStreamingExample", "org.example.advanced.StreamingExampleKt")