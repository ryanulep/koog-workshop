plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.koog.agents)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xskip-prerelease-check",
            "-Xexplicit-backing-fields"
        )
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
}
