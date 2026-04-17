package com.jetbrains.example.koog.compose.agents.calculator

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import com.jetbrains.example.koog.compose.agents.common.AgentProvider

/**
 * Factory for creating calculator agents
 */
internal class CalculatorAgentProvider(
    private val provideLLMClient: suspend () -> Pair<LLMClient, LLModel>,
) : AgentProvider {
    override val title: String = "Calculator"
    override val description: String = "Hi, I'm a calculator agent, I can do math"

    override suspend fun provideAgent(
        historyProvider: ChatHistoryProvider,
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
    ): AIAgent<String, String> {
        val (llmClient, model) = provideLLMClient.invoke()
        val executor = MultiLLMPromptExecutor(llmClient)

        val toolRegistry = ToolRegistry {
            tool(CalculatorTool.PlusTool)
            tool(CalculatorTool.MinusTool)
            tool(CalculatorTool.DivideTool)
            tool(CalculatorTool.MultiplyTool)
        }

        val strategy = strategy(title) {
            val nodeRequestLLM by nodeLLMRequestMultiple()
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            edge(nodeStart forwardTo nodeRequestLLM)

            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple
                    onMultipleToolCalls { true }
            )

            edge(
                nodeRequestLLM forwardTo nodeFinish
                    transformed { it.first() }
                    onAssistantMessage { true }
            )

            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory)
                    onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
            )

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                    onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
            )

            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                    onMultipleToolCalls { true }
            )

            edge(
                nodeSendToolResultMultiple forwardTo nodeFinish
                    transformed { it.first() }
                    onAssistantMessage { true }
            )
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("test") {
                system(
                    """
                    You are a calculator.
                    You will be provided math problems by the user.
                    Use tools at your disposal to solve them.
                    Provide the answer and ask for the next problem.
                    """.trimIndent()
                )
            },
            model = model,
            maxAgentIterations = 50
        )

        return AIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry,
        ) {
            install(ChatMemory) {
                chatHistoryProvider = historyProvider
                windowSize(50)
            }
            handleEvents {
                onToolCallStarting { ctx ->
                    onToolCallEvent("Tool ${ctx.toolName}, args ${ctx.toolArgs}")
                }

                onAgentExecutionFailed { ctx ->
                    onErrorEvent("${ctx.throwable.message}")
                }
            }
        }
    }
}
