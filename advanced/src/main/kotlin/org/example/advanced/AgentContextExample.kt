package org.example.advanced

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider

suspend fun main() {
    val openaiApiKey = System.getenv("OPENAI_API_KEY")

    MultiLLMPromptExecutor(
        LLMProvider.OpenAI to OpenAILLMClient(openaiApiKey)
    ).use { promptExecutor ->
        runAgent(promptExecutor)
    }

}

private suspend fun runAgent(
    promptExecutor: MultiLLMPromptExecutor,
) {
    val strategy = strategy<String, String>("example-strategy") {
//        val node1 by node<String, String> {
//            llm.readSession {
//                this
//            }
//        }
    }

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
        )
    )
}