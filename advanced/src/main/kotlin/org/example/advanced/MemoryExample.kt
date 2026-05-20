package org.example.advanced

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.asTool
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
import org.example.advanced.memory.LLMSummaryDocumentExtractor
import org.example.advanced.memory.LastUserMessagePromptAugmenter
import org.example.advanced.memory.LastUserResponseQueryProvider
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

    // max 2000 for pgvector vector type, otherwise you need to use a different vector type
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

    val sendMessageTool = ::sendMessage.asTool()

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        agentConfig = AIAgentConfig(
            prompt = prompt("example") {
                system {
                    +"You're a helpful assistant."
                    +"Use sendMessage tool to chat with the user, DO NOT send regular messages to chat."
                    +"Only send the normal message to end the conversation when the user is done."
                }
            },
            model = OpenAIModels.Chat.GPT5_5,
            maxAgentIterations = 100,
        ),
        toolRegistry = ToolRegistry {
            tool(sendMessageTool)
        }
    ) {
        install(LongTermMemory) {
            retrievalSettings = RetrievalSettings(
                storage = vectorStore,
                // How the prompt is going to be modified?
                promptAugmenter = LastUserMessagePromptAugmenter(),
                // How to construct a search query from the prompt?
                searchQueryProvider = LastUserResponseQueryProvider(sendMessageTool),
                // Search parameters
                searchStrategy = SimilaritySearchStrategy(
                    similarityThreshold = 0.2,
                )
            )

            ingestionSettings = IngestionSettings(
                storage = vectorStore,
                // custom document extractor based on LLM summarization
                documentExtractor = LLMSummaryDocumentExtractor(
                    promptExecutor = promptExecutor,
                )
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

    val initialMessage = sendMessage("Hi, how can I help you?")
    val result = agent.run(initialMessage)
    println("Agent: $result")
}

@Tool
@LLMDescription("Shows a message to the user and returns the user response. Call this tool to chat with the user.")
fun sendMessage(message: String): String {
    println("Agent: $message")
    print("User: ")
    return readln()
}

