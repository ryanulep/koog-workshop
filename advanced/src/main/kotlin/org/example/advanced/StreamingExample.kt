package org.example.advanced

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponse

class InfoTools : ToolSet {
    private val capitals = mapOf(
        "france" to "Paris",
        "germany" to "Berlin",
        "japan" to "Tokyo",
        "australia" to "Canberra",
        "brazil" to "Brasília",
    )
    private val populations = mapOf(
        "france" to "68 million",
        "germany" to "84 million",
        "japan" to "125 million",
        "australia" to "26 million",
        "brazil" to "215 million",
    )

    @Tool("get_capital")
    @LLMDescription("Get the capital city of a country")
    fun getCapital(
        @LLMDescription("Country name in lowercase, e.g. 'france'")
        country: String,
    ): String = capitals[country.lowercase()] ?: "Unknown capital for '$country'"

    @Tool("get_population")
    @LLMDescription("Get the approximate population of a country")
    fun getPopulation(
        @LLMDescription("Country name in lowercase, e.g. 'france'")
        country: String,
    ): String = populations[country.lowercase()] ?: "Unknown population for '$country'"
}

suspend fun main() {
    val openaiApiKey = System.getenv("OPENAI_API_KEY")

    MultiLLMPromptExecutor(
        LLMProvider.OpenAI to OpenAILLMClient(openaiApiKey)
    ).use { promptExecutor ->
        runStreamingExample(promptExecutor)
    }
}

/*
 * STREAMING WITH ON-THE-FLY TOOL EXECUTION
 *
 * The node owns the full agentic loop inside a single llm.writeSession { }.
 * Each iteration streams a response and acts on individual frames as they arrive:
 *
 *   StreamFrame.TextDelta        — incremental text chunk; printed immediately so
 *                                  the response appears word-by-word in the console
 *   StreamFrame.ToolCallComplete — a fully assembled tool invocation (id, name, JSON args);
 *                                  the tool is executed right away, before the stream ends
 *   StreamFrame.End              — marks the end of one streaming turn
 *
 * After each turn the assembled Message.Assistant (built from all collected frames
 * via toMessageResponse()) is committed to the prompt. If tool calls were made their
 * results are appended and the loop requests another streaming turn so the LLM can
 * incorporate the results. The loop exits once the LLM replies with plain text only.
 *
 * Note: environment (AIAgentEnvironment) is captured from the node context BEFORE
 * entering writeSession, because writeSession scope does not expose it directly.
 */
private suspend fun runStreamingExample(promptExecutor: MultiLLMPromptExecutor) {
    val toolRegistry = ToolRegistry { tools(InfoTools()) }

    val strategy = strategy<String, String>("streaming") {
        val nodeStream by node<String, String> { query ->
            // Capture environment here — it is not accessible inside writeSession
            val env = environment
            var finalText = ""

            llm.writeSession {
                appendPrompt { user(query) }

                while (true) {
                    val frames = mutableListOf<StreamFrame>()
                    val toolResults = mutableListOf<ReceivedToolResult>()
                    var hasText = false

                    requestLLMStreaming().collect { frame ->
                        frames.add(frame)
                        when (frame) {
                            is StreamFrame.TextDelta -> {
                                // Print each token as it arrives — the response appears gradually
                                print(frame.text)
                                hasText = true
                            }

                            is StreamFrame.ToolCallComplete -> {
                                // Execute the tool immediately when its complete frame arrives,
                                // without waiting for the stream to finish
                                val call = MessagePart.Tool.Call(frame.id, frame.name, frame.content)
                                println("\n  [tool call] ${frame.name}(${frame.content})")
                                val result = env.executeTool(call)
                                println("  [tool result] ${result.output}")
                                toolResults.add(result)
                            }

                            is StreamFrame.End -> if (hasText) println()

                            else -> {}
                        }
                    }

                    // Commit the full assistant message (text + tool calls) to the prompt
                    appendPrompt { message(frames.toMessageResponse()) }

                    if (toolResults.isEmpty()) {
                        // No tool calls this turn — the streamed text is the final answer
                        finalText = frames.filterIsInstance<StreamFrame.TextComplete>()
                            .joinToString("") { it.text }
                        break
                    }

                    // Send tool results back so the LLM can use them in the next turn
                    appendPrompt {
                        toolResults.forEach { result -> toolResult(result.toMessagePart()) }
                    }
                }
            }

            finalText
        }

        nodeStart then nodeStream then nodeFinish
    }

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = strategy,
        agentConfig = AIAgentConfig(
            prompt = prompt("streaming") {
                system { +"You are a geography assistant. Use tools to look up data, then summarise." }
            },
            model = OpenAIModels.Chat.GPT4oMini,
            maxAgentIterations = 20,
        ),
        toolRegistry = toolRegistry,
    )

    println("Query: What are the capitals and populations of France, Germany, and Japan?\n")
    val result = agent.run("What are the capitals and populations of France, Germany, and Japan?")
    println("\nFinal answer: $result")
}
