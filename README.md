# Koog Workshop

A hands-on introduction to the [Koog](https://github.com/JetBrains/koog) agent framework for Kotlin. The workspace contains five Gradle modules, each targeting a different level of complexity.

## Modules

| Module      | What it is                                                           |
|-------------|----------------------------------------------------------------------|
| `:intro`    | Compose Desktop app with guided agent demos (weather, home services) |
| `:advanced` | Four standalone CLI examples covering some more advanced Koog APIs   |
| `:app`      | Compose Desktop fantasy-store client (connects to `:server`)         |
| `:server`   | Spring Boot backend with Koog agent, SSE streaming, and REST API     |
| `:shared`   | Serializable models shared between `:app` and `:server`              |

Start with `:intro` or `:advanced` if you are new to Koog. `:app` + `:server` together form a complete client-server application.

## Requirements

- JDK compatible with Gradle and Compose Desktop
- `OPENAI_API_KEY` environment variable (or an `env.properties` file for `:advanced`)

## :intro — introductory demos

An interactive Compose Desktop app (1200×800) with two built-in agent demos selectable from the start screen:

- **Weather agent** — tool-based API integration
- **Home Services agent** — multi-step booking workflow using a graph-based strategy

```sh
export OPENAI_API_KEY=your_key_here
./gradlew :intro:run
```

A Settings screen in the app lets you enter the API key through the UI instead.

## :advanced — standalone examples

Four CLI programs that each focus on one Koog concept. Create an `advanced/env.properties` file with `OPENAI_API_KEY=your_key_here` before running.

| Task                                            | File                         | What it shows                                                                                                                          |
|-------------------------------------------------|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `./gradlew advanced:runStreamingExample`        | `StreamingExample.kt`        | Real-time token streaming; processing `StreamFrame` events and executing tools as they arrive                                          |
| `./gradlew advanced:runStructuredOutputExample` | `StructuredOutputExample.kt` | Extracting structured data three ways: automatic schema, native OpenAI `response_format`, and manual schema with error-correction loop |
| `./gradlew advanced:runAgentContextExample`     | `AgentContextExample.kt`     | Agent context APIs: `agentInput`, `storage`, `llm.writeSession`, `llm.readSession`, and detached `promptExecutor` calls                |
| `./gradlew advanced:runMemoryExample`           | `MemoryExample.kt`           | Long-term memory with vector embeddings via pgvector; requires `docker-compose up`                                                     |

The memory example uses the PostgreSQL + pgvector service defined in `advanced/docker-compose.yml`. Langfuse observability services are defined there as well.

## :app + :server — fantasy store

A complete client-server demo. The `:server` must be running before you start `:app`.

### Server

Spring Boot REST API with SQLite persistence and Koog agent integration. On first start it creates `.agent-fantasy-store/agent-fantasy-store.db` and seeds demo data.

```sh
export OPENAI_API_KEY=your_key_here
./gradlew :server:bootRun
```

Starts on `http://localhost:8080`. Key endpoints:

- `POST /chat` — streams agent responses as SSE
- `POST /chat/answer` — sends a reply when the agent asks a follow-up question
- `GET /chat/state` — returns the current agent state for a session
- Admin CRUD under `/admin/**`

### Desktop client

Compose Desktop app with a customer-support chat window and an admin workspace.

```sh
export OPENAI_API_KEY=your_key_here
./gradlew :app:run
```

- Main window: customer chat. Select a character from the top bar to enable order-specific tools.
- Admin window: product, merchant, and order management. Open from the top bar.

### Tests

```sh
./gradlew :app:test
```

## Project layout

```
intro/          Introductory Compose Desktop app
advanced/       Standalone CLI examples
app/            Fantasy-store Compose Desktop client
server/         Spring Boot backend
shared/         Serializable models (used by app + server)
docker-compose.yml  PostgreSQL/pgvector + Langfuse observability stack
```
