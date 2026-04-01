import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

val configurationName = gradle.startParameter.projectProperties["codexConfigurationName"] ?: "runtimeClasspath"

fun File.normalizedPath(): String = absoluteFile.normalize().path.replace('\\', '/')

fun Project.resolvedConfiguration(name: String) =
    configurations.findByName(name)
        ?: error(
            buildString {
                append("Configuration '")
                append(name)
                append("' not found in ")
                append(path)
                append(". Available resolvable configurations: ")
                append(
                    configurations
                        .filter { it.isCanBeResolved }
                        .map { it.name }
                        .sorted()
                        .joinToString(", ")
                )
            }
        )

fun Project.mainOutputFiles(): List<File> =
    extensions
        .findByType(SourceSetContainer::class.java)
        ?.findByName("main")
        ?.output
        ?.files
        ?.map { it.absoluteFile.normalize() }
        ?.filter { it.exists() }
        ?.distinct()
        ?.sortedBy { it.path }
        .orEmpty()

allprojects {
    tasks.register("codexListResolvableConfigurations") {
        doLast {
            configurations
                .filter { it.isCanBeResolved }
                .map { it.name }
                .sorted()
                .forEach(::println)
        }
    }

    tasks.register("codexPrintResolvedFiles") {
        doLast {
            val configuration = resolvedConfiguration(configurationName)

            if (!configuration.isCanBeResolved) {
                error("Configuration '$configurationName' exists in $path but is not resolvable.")
            }

            configuration
                .resolve()
                .map { it.absoluteFile.normalize() }
                .distinct()
                .sortedBy { it.path }
                .forEach { println(it.normalizedPath()) }
        }
    }

    tasks.register("codexPrintRuntimeInfo") {
        doLast {
            mainOutputFiles().forEach { println("OUTPUT\t${it.normalizedPath()}") }

            val configuration = resolvedConfiguration(configurationName)
            if (!configuration.isCanBeResolved) {
                error("Configuration '$configurationName' exists in $path but is not resolvable.")
            }

            configuration
                .resolve()
                .map { it.absoluteFile.normalize() }
                .distinct()
                .sortedBy { it.path }
                .forEach { println("DEPENDENCY\t${it.normalizedPath()}") }
        }
    }
}
