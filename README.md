# Agent Fantasy Store

Agent Fantasy Store is a single-module Kotlin/JVM desktop application built with Compose Multiplatform. It combines a customer-support chat experience, an admin workspace, and a local SQLite database seeded with demo fantasy-store data.

## What is in the app

- **Customer support chat** powered by Koog and an OpenAI executor.
- **Character-aware order support** after selecting a character in the UI.
- **Admin workspace** for product operations, merchant operations, shipping assignments, and order management.
- **Local persistence** with SQLite and Exposed v1.
- **Seeded demo data** for characters, merchants, shipping methods, products, orders, and transaction history.

## Tech stack

- Kotlin/JVM
- Compose Multiplatform Desktop + Material 3
- AndroidX Lifecycle ViewModel
- Koog agents + persisted chat history
- SQLite + Exposed v1
- Coroutines + `kotlin.test`/JUnit-based test suites

## Requirements

- A working JDK supported by Gradle and Compose Desktop
- `OPENAI_API_KEY` set in the environment before launching the app

The app fails fast on startup if `OPENAI_API_KEY` is missing.

## Run the app

Always use Gradle from the wrapper:

```sh
export OPENAI_API_KEY=your_key_here
./gradlew :app:run
```

On first run the app:

- creates a local SQLite database at `.agent-fantasy-store/agent-fantasy-store.db`
- creates the schema automatically
- seeds demo data if the main store tables are empty

## Run tests

```sh
./gradlew :app:test
```

## How the UI works

- The main window is the customer-support chat.
- The top bar lets you open the admin workspace.
- The top bar also lets you select a character.
- Without a selected character, the assistant handles general store questions.
- With a selected character, the assistant can use order-specific tools to inspect and update that character's orders.

## Project structure

```text
app/src/main/kotlin/org/example/project/
├── main.kt                 # Desktop entry point, launches chat and admin windows
├── dependencies.kt         # Manual dependency wiring
├── chat/                   # Chat UI, state, and Koog-backed agent
├── admin/                  # Admin screens, view models, repositories, services
├── domain/                 # Store domain models, repositories, and services
├── db/                     # SQLite setup, schema creation, and demo data seeding
└── koog/                   # Agent tools, strategies, and chat history integration
```

Main domain areas currently implemented:

- catalog
- orders
- characters
- cart
- currency
- reviews
- shipping
- wishlist

## Architecture notes

- The project contains a single Gradle module: `:app`.
- Persistence uses Exposed v1 and wraps database work in suspend transactions.
- Chat history is stored in SQLite through `JdbcChatHistoryProvider`.
- The desktop packaging configuration targets DMG, MSI, and DEB distributions.

## Tests

The test suite covers more than just the UI shell. Existing tests include:

- database schema and repository integration
- domain service behavior
- admin view models
- compose integration helpers for the desktop app

Tests live under `app/src/test/kotlin/org/example/project/`.

## Status

This repository is not the default Compose template anymore. The previous README described a non-existent `composeApp` module; the real application lives in `:app` and is focused on the fantasy store desktop experience described above.
