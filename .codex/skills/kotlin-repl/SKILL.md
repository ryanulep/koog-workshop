---
name: kotlin-repl
description: Launch and use a Kotlin REPL with the project classpath loaded to prototype snippets, inspect runtime behavior, and debug small pieces of Kotlin code. Use when you need to experiment with Kotlin APIs, validate Exposed or Compose behavior, or try code against compiled project outputs before editing sources.
---

# Kotlin REPL

Use this skill to validate small Kotlin changes against real compiled code before editing source files.

## Workflow

1. Pick the narrowest module scope that covers the code under test.
2. Refresh the build inputs first so the REPL sees current compiled outputs.
3. Load the target module outputs plus the needed dependency jars on the classpath.
4. Open the REPL and test one hypothesis at a time.
5. Translate the validated snippet into a source change or test.

## Classpath Rules

- Prefer compiled outputs over raw source directories.
- Include the module under test and only the project modules it depends on.
- Use runtime classpaths when the code depends on runtime-only libraries or service wiring.
- Keep the classpath minimal so startup stays fast and ambiguous symbol resolution is easier to spot.

## Repo Guidance

- Prefer Gradle MCP for build inspection when it is available.
- Fall back to the Gradle wrapper if MCP is unavailable.
- In this repo, `domain` holds most business logic and `composeApp` holds the desktop UI entry point.
- Common compiled output roots are `module/build/classes/kotlin/jvm/main` and `module/build/resources/main`.
- Start with `domain` alone for business logic probes; add `composeApp` when you need UI, app wiring, or desktop-specific code.

## Good REPL Uses

- Verify how an Exposed v1 query behaves with live data shapes.
- Check extension resolution, nullability, and overload selection.
- Inspect service outputs before changing a repository or UI layer.
- Prototype a transformation and then carry the working snippet back into production code.

## Avoid

- Large end-to-end flows that are clearer as tests.
- Experiments that need deterministic assertions across many cases.
- Repeating the same snippet without narrowing the inputs or the classpath.

## Details

See [references/repl-workflow.md](references/repl-workflow.md) for the concrete launch pattern and session discipline.
