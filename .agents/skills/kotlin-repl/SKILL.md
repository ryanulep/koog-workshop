---
name: kotlin-repl
description: Launch and use a Kotlin/JVM REPL against a real project classpath to prototype snippets, inspect runtime behavior, validate imports and overloads, and debug small pieces of Kotlin code before editing source files or writing tests. Use when Codex needs to check a Kotlin hypothesis against compiled project outputs in a Gradle JVM or Kotlin Multiplatform JVM target, or must resolve the full REPL classpath and dependency jars automatically.
---

# Kotlin REPL

Use this skill to validate small Kotlin/JVM ideas against compiled project outputs before editing source files.

## Workflow

1. Pick the narrowest JVM-capable module or target that covers the code under test.
2. Run the Gradle launcher from inside that module directory when possible. If you are starting from the repo root, set `MODULE=<module>` as the skill argument.
3. Let the Gradle launcher compile the target, resolve the runtime configuration, and collect both module outputs and dependency jars automatically.
4. Launch the REPL, prove one hypothesis, and record the exact snippet that worked.
5. Translate the validated snippet into source code or a test.

## Scope Rules

- Use this skill for Kotlin/JVM code or a JVM target of a multiplatform project.
- If the code lives in `commonMain`, still compile and load a concrete JVM target before using the REPL.
- Prefer a test or small runnable program instead when behavior depends on long flows, concurrency, or stable assertions across many cases.

## Classpath Rules

- Prefer compiled outputs over raw source directories.
- Put the module under test first on the classpath.
- Add resource directories only when they exist and the snippet needs them.
- Resolve dependency jars from Gradle rather than guessing jar paths manually.
- For a plain JVM module, start from `runtimeClasspath`.
- For a Kotlin Multiplatform JVM target, start from `jvmRuntimeClasspath`.
- Keep the classpath minimal so startup stays fast and symbol resolution stays clear.

## Launch Rules

- Launch the session with `kotlinc -Xrepl`, not `kotlin -Xrepl`.
- Keep REPL state inside the current project by setting `-J-Duser.home` to a build-owned directory such as `build/codex-repl` or `<module>/build/codex-repl`.
- Resolve `<skill-dir>` in the examples below to the directory that contains this `SKILL.md`.
- Prefer [scripts/start-gradle-repl.sh](scripts/start-gradle-repl.sh) for Gradle projects. It auto-detects the nearest `./gradlew`, infers the module from the current working directory or `MODULE=<module>`, asks Gradle for the main output directories when available, resolves dependency jars from the runtime configuration, warns about Kotlin version mismatches on the resolved classpath, and then launches the REPL.
- Use [scripts/start-repl.sh](scripts/start-repl.sh) only when you already know the classpath and want the low-level launcher.
- Do not run `:dependencies`, search `~/.gradle/caches`, or manually locate `build/classes/...` unless the Gradle configuration itself is broken. The Gradle launcher is the default path.
- If you need `kotlin.uuid.Uuid.parse(...)` or similar experimental APIs in the REPL, put the code inside a helper function annotated with `@OptIn(...)`; top-level opt-in annotations may not carry the way you expect between REPL snippets.

## Gradle JVM Example

From inside the module directory:

```bash
cd "$PROJECT_DIR/domain"
bash <skill-dir>/scripts/start-gradle-repl.sh
```

From the repo root, pass the module as the skill argument:

```bash
MODULE=domain bash <skill-dir>/scripts/start-gradle-repl.sh
```

Inspect or reuse the generated classpath without launching the REPL:

```bash
MODULE=domain bash <skill-dir>/scripts/start-gradle-repl.sh --print-classpath
```

## Gradle KMP/JVM Example

Use a concrete JVM target when the code lives in `commonMain`:

```bash
MODULE=shared bash <skill-dir>/scripts/start-gradle-repl.sh --kind kmp-jvm
```

If the project does not use the default `jvm` target names, list the available runtime configurations first and then override the defaults:

```bash
MODULE=shared bash <skill-dir>/scripts/start-gradle-repl.sh --list-configurations
MODULE=shared bash <skill-dir>/scripts/start-gradle-repl.sh \
  --kind kmp-jvm \
  --configuration desktopRuntimeClasspath \
  --build-task compileKotlinDesktop \
  --classpath-entry "$PWD/$MODULE/build/classes/kotlin/desktop/main"
```

Then start with a small probe:

```kotlin
import your.pkg.TypeUnderTest

fun demoValue(): TypeUnderTest = TODO("construct the smallest representative value")
println(demoValue())
```

## Good REPL Uses

- Verify how a library call behaves on the real project classpath.
- Check extension resolution, nullability, and overload selection.
- Inspect service outputs before changing a repository or UI layer.
- Prototype a transformation and then carry the working snippet back into production code.

## Avoid

- Large end-to-end flows that are clearer as tests.
- Experiments that need deterministic assertions across many cases.
- Repeating the same snippet without narrowing the inputs or the classpath.

## Details

See [references/repl-workflow.md](references/repl-workflow.md) for the concrete launch pattern and session discipline.
