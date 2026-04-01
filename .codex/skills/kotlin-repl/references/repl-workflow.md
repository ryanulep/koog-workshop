# Kotlin REPL Workflow

Use a REPL when you need to validate a small Kotlin idea against the actual compiled project outputs.

## Choose the scope

- Start with the smallest module that covers the code.
- Add project modules only when the snippet touches them.
- Prefer `domain` for pure business logic.
- Add `composeApp` only when you need desktop UI or app wiring.

## Refresh the build

- Use Gradle MCP to inspect the build graph and source sets when it is available.
- Otherwise, compile the target module first with the Gradle wrapper.
- Keep the module current before starting the REPL so you are testing real outputs.

## Build the classpath

- Put compiled module outputs first.
- Add the matching `build/resources/main` directory when the module needs resource loading.
- Append dependency jars from the relevant compile or runtime classpath.
- Avoid loading both `domain` and `composeApp` unless the snippet actually needs both.

Example roots for this repo:

- `domain/build/classes/kotlin/jvm/main`
- `domain/build/resources/main`
- `composeApp/build/classes/kotlin/jvm/main`
- `composeApp/build/resources/main`

## Launch the session

- Start `kotlin` with the assembled classpath.
- Keep the first input small: imports, one helper function, one assertion or print.
- Use `println` and small sample values to confirm behavior.
- Reset the session when switching to a different experiment.

## Session discipline

- Test one hypothesis per session.
- Narrow the input shape before changing the code path.
- If the session becomes noisy, stop and restart with a smaller classpath.
- Record the exact snippet that proved the behavior before translating it into source.
