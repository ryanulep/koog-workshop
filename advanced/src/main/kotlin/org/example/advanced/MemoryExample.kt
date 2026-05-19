package org.example.advanced

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.longtermmemory.feature.LongTermMemory
import ai.koog.agents.longtermmemory.ingestion.IngestionSettings
import ai.koog.agents.longtermmemory.retrieval.RetrievalSettings
import ai.koog.agents.longtermmemory.retrieval.search.SimilaritySearchStrategy
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.rag.vector.storage.JVMFileEmbeddingStorage
import ai.koog.spring.ai.vectorstore.SpringAiKoogVectorStore
import kotlinx.coroutines.Dispatchers
import org.example.advanced.memory.UserPromptAugmenterFixed
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import kotlin.io.path.Path
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
suspend fun main() {
    val openaiApiKey = System.getenv("OPENAI_API_KEY")

    MultiLLMPromptExecutor(
        LLMProvider.OpenAI to OpenAILLMClient(openaiApiKey)
    ).use { promptExecutor ->
        runSimpleAgent(promptExecutor, openaiApiKey)
    }
}

private fun provideSpringPgVectorStore(
    openaiApiKey: String,
): SpringAiKoogVectorStore {
    val dataSource = DriverManagerDataSource().apply {
        setDriverClassName("org.postgresql.Driver")
        url = "jdbc:postgresql://localhost:5432/postgres"
        username = "postgres"
        password = "postgres"
    }

    val embeddingDimensions = 1536

    val embeddingModel = OpenAiEmbeddingModel(
        OpenAiApi.builder().apiKey(openaiApiKey).build(),
        MetadataMode.EMBED,
        OpenAiEmbeddingOptions.builder()
            .model("text-embedding-3-small")
            .dimensions(embeddingDimensions)
            .build()
    )

    val pgVectorStore = PgVectorStore.builder(JdbcTemplate(dataSource), embeddingModel)
        .schemaName("public")
        .vectorTableName("spring_agent_memory")
        .dimensions(embeddingDimensions)
        .initializeSchema(true)
        .build()
        .also {
            // Need to call it manually, because we're not using Spring app, which usually calls this automatically
            it.afterPropertiesSet()
        }

    return SpringAiKoogVectorStore(pgVectorStore, Dispatchers.IO)
}

private fun provideFileVectorStorage(
    openAiApiKey: String,
): JVMFileEmbeddingStorage {
    return JVMFileEmbeddingStorage(
        LLMEmbedder(
            OpenAILLMClient(openAiApiKey),
            OpenAIModels.Embeddings.TextEmbedding3Small
        ),
        root = Path(".")
    )
}

private suspend fun runSimpleAgent(
    promptExecutor: MultiLLMPromptExecutor,
    openaiApiKey: String,
) {
    val vectorStore = provideSpringPgVectorStore(openaiApiKey)
    //val vectorStore = provideFileVectorStorage(openaiApiKey)

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig = AIAgentConfig(
            prompt = prompt("example") {
                system {
                    +"You're a helpful assistant."
                }
            },
            model = OpenAIModels.Chat.GPT5_4,
            maxAgentIterations = 50,
        ),
    ) {
        install(LongTermMemory) {
            retrievalSettings = RetrievalSettings(
                storage = vectorStore,
                promptAugmenter = UserPromptAugmenterFixed(),
                searchStrategy = SimilaritySearchStrategy(
                    similarityThreshold = 0.2,
                )
            )

            ingestionSettings = IngestionSettings(
                storage = vectorStore,
            )
        }

        install(EventHandler) {
            onLLMCallStarting { ctx ->
                 val prompt = ctx.prompt
            }

            onLLMCallCompleted { ctx ->
                val prompt = ctx.prompt
            }
        }
    }

    var userMessage = askUser("Hi, how can I help you?")

    while (userMessage != "/q") {
        val agentResponse = agent.run(userMessage)
        userMessage = askUser(agentResponse)
    }
}

private fun askUser(question: String): String {
    println("Agent: $question")
    print("User: ")
    return readln()
}

