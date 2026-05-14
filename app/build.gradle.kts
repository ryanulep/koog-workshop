import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
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

// Compose Compiler Metrics - Development/Debug Only
// Enable with: ./gradlew build -PenableComposeCompilerMetrics=true
// Or set enableComposeCompilerMetrics=true in gradle.properties
val enableMetrics = project.findProperty("enableComposeCompilerMetrics")?.toString()?.toBoolean() == true

if (enableMetrics) {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                        project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics"
            )
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                        project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics"
            )
        }
    }
    println("🔍 Compose Compiler Metrics ENABLED - Reports will be generated in: build/compose_metrics/")
} else {
    println("ℹ️  Compose Compiler Metrics DISABLED - Enable with -PenableComposeCompilerMetrics=true")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(compose.desktop.currentOs)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.koog.agents)
    implementation(libs.koog.memory)
    implementation(libs.markdown.renderer)
    implementation(libs.logback.classic)
    implementation(ktorLibs.client.okhttp)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.client.logging)
//    implementation(ktorLibs.client.sse)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.compose.uiTest)
    testImplementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
