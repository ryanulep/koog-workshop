# Kotlin REPL Workflow

Use a REPL when you need to validate a small Kotlin/JVM idea against compiled project outputs.

## 1. Pick a JVM target

- Choose the smallest JVM-capable module or source set that contains the code.
- For multiplatform code, compile a concrete JVM target such as `jvmMain`, desktop, or server. Do not point the REPL at `commonMain` alone.
- Skip the REPL for Native-only or JS-only code.
- When you are at the repo root, pass `MODULE=<module>` to the launcher. When you are already inside the module directory, let the launcher infer the module from the current working directory.

## 2. Refresh compiled outputs

- Prefer this skill's Gradle launcher for Gradle projects. It compiles the target first so you are testing current outputs.
- Use the project's normal build tool. Prefer the narrowest task that refreshes the code you need.
- Gradle JVM defaults: `:module:classes` with `runtimeClasspath`
- Gradle KMP/JVM defaults: `:module:compileKotlinJvm` with `jvmRuntimeClasspath`
- Maven example: `mvn -pl <module> -DskipTests compile`

## 3. Assemble the classpath

- Put compiled module outputs first.
- Add resources only when the directory exists and the snippet needs them.
- Resolve dependency jars from the build tool rather than guessing file paths manually.
- For Gradle projects, use `scripts/start-gradle-repl.sh` so the runtime jars come from the resolved configuration and the module outputs come from Gradle or the standard build directories instead of a hand-built classpath.

Common output roots:

- Gradle JVM: `<module>/build/classes/kotlin/main`
- Gradle multiplatform JVM: `<module>/build/classes/kotlin/jvm/main`
- Java interop: `<module>/build/classes/java/main`
- Java interop for KMP/JVM: `<module>/build/classes/java/jvm/main`
- Gradle resources: `<module>/build/resources/main`
- Gradle KMP resources: `<module>/build/processedResources/jvm/main` or `<module>/build/resources/jvm/main`
- Maven JVM: `<module>/target/classes`

## 4. Launch the session

- Start `kotlinc -Xrepl` with the assembled classpath.
- Keep REPL state inside the current project by setting `HOME` and `-J-Duser.home` to a build-owned directory.
- Resolve `<skill-dir>` in the example below to the directory that contains this skill.
- For Gradle projects, prefer `scripts/start-gradle-repl.sh` instead of assembling the classpath yourself.
- If you already know the classpath and want the low-level launcher only, use `scripts/start-repl.sh`.
- Keep the first input small: imports, one helper function, one assertion or print.
- Use `println` and small sample values to confirm behavior.
- Reset the session when switching to a different experiment.

Gradle JVM example:

```bash
cd "$PROJECT_DIR/domain"
bash <skill-dir>/scripts/start-gradle-repl.sh
```

Inspect the resolved classpath without launching:

```bash
MODULE=domain bash <skill-dir>/scripts/start-gradle-repl.sh --print-classpath
```

Gradle KMP/JVM example:

```bash
MODULE=shared bash <skill-dir>/scripts/start-gradle-repl.sh --kind kmp-jvm
```

Discover configuration names when the project uses a custom JVM target:

```bash
MODULE=shared bash <skill-dir>/scripts/start-gradle-repl.sh --list-configurations
```

Override the defaults for a custom target name:

```bash
MODULE=shared bash <skill-dir>/scripts/start-gradle-repl.sh \
  --kind kmp-jvm \
  --configuration desktopRuntimeClasspath \
  --build-task compileKotlinDesktop \
  --classpath-entry "$PWD/shared/build/classes/kotlin/desktop/main"
```

## 5. Session discipline

- Test one hypothesis per session.
- Narrow the input shape before changing the code path.
- If the session becomes noisy, stop and restart with a smaller classpath.
- Record the exact snippet that proved the behavior before translating it into source.

## 6. Kotlin version mismatches

- A standalone `kotlinc` binary can lag behind the Kotlin stdlib jars on the project's Gradle classpath.
- `scripts/start-gradle-repl.sh` warns when the resolved classpath mixes Kotlin stdlib versions, or when `kotlinc -version` does not match the resolved stdlib version.
- If you hit metadata or symbol errors, point the script at the matching compiler with `--kotlinc <path>`.
