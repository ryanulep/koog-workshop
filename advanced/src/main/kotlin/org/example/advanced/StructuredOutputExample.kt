package org.example.advanced

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructuredWithUserText
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openai.base.structure.OpenAIStandardJsonSchemaGenerator
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.StructuredResponse
import ai.koog.prompt.structure.json.JsonStructure
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MovieReview")
@LLMDescription("Structured extraction of a movie review")
data class MovieReview(
    @property:LLMDescription("Movie title") val title: String,
    @property:LLMDescription("Rating out of 10") val rating: Int,
    @property:LLMDescription("List of genres") val genres: List<String>,
    @property:LLMDescription("One-sentence summary of the review") val summary: String,
    @property:LLMDescription("Overall sentiment: POSITIVE, MIXED, or NEGATIVE") val sentiment: String,
)

suspend fun main() {
    val openaiApiKey = System.getenv("OPENAI_API_KEY")

    MultiLLMPromptExecutor(
        LLMProvider.OpenAI to OpenAILLMClient(openaiApiKey)
    ).use { promptExecutor ->
        val reviewText = """
            I finally watched Inception last night and I can't stop thinking about it.
            Nolan's direction is superb and the performances — especially DiCaprio's — are captivating.
            The score by Hans Zimmer is iconic. My only gripe is that the final act drags slightly,
            but overall this is a masterpiece of sci-fi cinema. Easily a 9/10.
        """.trimIndent()

        runAutomaticAgent(promptExecutor, reviewText)
        runNativeAgent(promptExecutor, reviewText)
        runManualAgent(promptExecutor, reviewText)
    }
}

private suspend fun runAutomaticAgent(promptExecutor: MultiLLMPromptExecutor, reviewText: String) {
    val strategy = strategy<String, MovieReview>("structured-native") {
        val nodeExtract by nodeLLMRequestStructuredWithUserText<MovieReview>()

        val nodeUnwrap by node<Result<StructuredResponse<MovieReview>>, MovieReview> { result ->
            result.getOrThrow().data
        }

        nodeStart then nodeExtract then nodeUnwrap then nodeFinish
    }

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = strategy,
        agentConfig = AIAgentConfig(
            prompt = prompt("structured-native") {
                system { +"Extract structured information from the movie review provided by the user." }
            },
            model = OpenAIModels.Chat.GPT4oMini,
            maxAgentIterations = 10,
        ),
    )

    val result = agent.run(reviewText)
    println("[automatic]  $result")
}

/*
 * NATIVE STRUCTURED OUTPUT
 *
 * StructuredRequest.Native<T> uses the model's built-in structured output capability
 * (OpenAI response_format / JSON schema parameter). The schema is passed as a model
 * parameter — no extra user message is injected into the prompt.
 *
 * Steps:
 *   1. Create a JsonStructure<T> from the @Serializable data class via JsonStructure.create<T>().
 *      This auto-generates the JSON schema from the class and its @LLMDescription annotations.
 *   2. Wrap it in StructuredRequestConfig, mapping the provider to StructuredRequest.Native.
 *   3. Use nodeLLMRequestStructuredWithUserText(config) — a pre-built node that appends the input as a
 *      user message and calls requestLLMStructured internally.
 *   4. nodeLLMRequestStructured returns Result<StructuredResponse<T>>; unwrap in a follow-up node.
 */
private suspend fun runNativeAgent(promptExecutor: MultiLLMPromptExecutor, reviewText: String) {
    val structure = JsonStructure.create<MovieReview>(
        schemaGenerator = OpenAIStandardJsonSchemaGenerator,
        // examples = listOf() // you can provide examples here to guide the model
    )

    val config = StructuredRequestConfig(
        byProvider = mapOf(LLMProvider.OpenAI to StructuredRequest.Native(structure))
    )

    val strategy = strategy<String, MovieReview>("structured-native") {
        val nodeExtract by nodeLLMRequestStructuredWithUserText(config = config)

        val nodeUnwrap by node<Result<StructuredResponse<MovieReview>>, MovieReview> { result ->
            result.getOrThrow().data
        }

        nodeStart then nodeExtract then nodeUnwrap then nodeFinish
    }

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = strategy,
        agentConfig = AIAgentConfig(
            prompt = prompt("structured-native") {
                system { +"Extract structured information from the movie review provided by the user." }
            },
            model = OpenAIModels.Chat.GPT4oMini,
            maxAgentIterations = 10,
        ),
    )

    val result = agent.run(reviewText)
    println("[native]  $result")
}

/*
 * MANUAL STRUCTURED OUTPUT
 *
 * StructuredRequest.Manual<T> does NOT use the model's built-in JSON mode. Instead, it
 * injects an explicit user message describing the expected JSON schema (generated from the
 * structure definition) before calling the LLM. This works with any model, including those
 * that lack native JSON mode support.
 *
 * StructureFixingParser adds a safety net: if the model returns malformed JSON, it makes up
 * to `retries` additional LLM calls, each time providing the parse error and asking the model
 * to correct the output. This is the "manual" repair loop that gives the approach its name.
 *
 * Steps:
 *   1. Same JsonStructure.create<T>() as in the native case.
 *   2. Use StructuredRequest.Manual (set as default so it applies to any provider).
 *   3. Pass a StructureFixingParser to nodeLLMRequestStructured to enable error correction.
 *   4. Unwrap the result identically.
 */
private suspend fun runManualAgent(promptExecutor: MultiLLMPromptExecutor, reviewText: String) {
    val structure = JsonStructure.create<MovieReview>(
        schemaGenerator = OpenAIStandardJsonSchemaGenerator,
        // examples = listOf() // you can provide examples here to guide the model
    )

    val config = StructuredRequestConfig(
        default = StructuredRequest.Manual(structure)
    )
    val fixingParser = StructureFixingParser(
        model = OpenAIModels.Chat.GPT4oMini,
        retries = 2,
    )

    val strategy = strategy<String, MovieReview>("structured-manual") {
        val nodeExtract by nodeLLMRequestStructuredWithUserText(config = config, fixingParser = fixingParser)

        val nodeUnwrap by node<Result<StructuredResponse<MovieReview>>, MovieReview> { result ->
            result.getOrThrow().data
        }

        nodeStart then nodeExtract then nodeUnwrap then nodeFinish
    }

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = strategy,
        agentConfig = AIAgentConfig(
            prompt = prompt("structured-manual") {
                system { +"Extract structured information from the movie review provided by the user." }
            },
            model = OpenAIModels.Chat.GPT4oMini,
            maxAgentIterations = 10,
        ),
    )

    val result = agent.run(reviewText)
    println("[manual]  $result")
}
