package org.example.advanced

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.longtermmemory.model.MemoryRecord
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.rag.base.storage.search.SearchRequest
import ai.koog.rag.base.storage.search.SimilaritySearchRequest
import ai.koog.spring.ai.vectorstore.SpringAiKoogVectorStore
import kotlinx.coroutines.Dispatchers
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.ai.document.MetadataMode
import com.pgvector.PGvector
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.jdbc.datasource.DriverManagerDataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
suspend fun main() {
    val openaiApiKey = System.getenv("OPENAI_API_KEY")

    MultiLLMPromptExecutor(
        LLMProvider.OpenAI to OpenAILLMClient(openaiApiKey)
    ).use { promptExecutor ->
        val dataSource = object : DelegatingDataSource(DriverManagerDataSource().apply {
            setDriverClassName("org.postgresql.Driver")
            url = "jdbc:postgresql://localhost:5432/postgres"
            username = "postgres"
            password = "postgres"
        }) {
            override fun getConnection() =
                super.getConnection().also(PGvector::addVectorType)

            override fun getConnection(u: String, p: String) =
                super.getConnection(u, p).also(PGvector::addVectorType)
        }

        val embeddingModel = OpenAiEmbeddingModel(
            OpenAiApi.builder().apiKey(openaiApiKey).build(),
            MetadataMode.EMBED,
            OpenAiEmbeddingOptions.builder()
                .model("text-embedding-3-small")
                .dimensions(1536)
                .build()
        )

        val pgVectorStore = PgVectorStore.builder(JdbcTemplate(dataSource), embeddingModel)
            .schemaName("public")
            .vectorTableName("spring_agent_memory")
            .dimensions(1536)
            .initializeSchema(true)
            .build()
            .also {
                // Need to call it manually, because we're not using Spring app, which usually calls this automatically
                it.afterPropertiesSet()
            }

        val vectorStore = SpringAiKoogVectorStore(pgVectorStore, Dispatchers.IO)

/*
        vectorStore.add(
            listOf(
                MemoryRecord("I love pizza", Uuid.random().toString()),
                MemoryRecord("I hate corn", Uuid.random().toString()),
            )
        )
*/

/*
        vectorStore.add(
            listOf(
                MemoryRecord(
                    content = "I like doing calisthenics",
                    id = Uuid.random().toString(),
                    metadata = mapOf("source" to "chat")
                ),
            )
        )
*/

        val result = vectorStore.search(
            SimilaritySearchRequest(
                queryText = "Which food I don't like?",
                minScore = 0.3,
            )
        )

        result.forEach { println(it) }

/*
        val agent = AIAgent(
            promptExecutor = promptExecutor,
            agentConfig = AIAgentConfig(
                prompt = prompt("example") {
                    system("You're a helpful assistant.")
                },
                model = OpenAIModels.Chat.GPT5_4,
                maxAgentIterations = 50,
            ),
        )

        val result = agent.run("Aye, mate!")
        println(result)
*/
    }
}