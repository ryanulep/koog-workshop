package com.jetbrains.koog.workshop.settings

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel

object ApiKeyService {
    enum class ServiceProvider {
        ANTHROPIC, OPENAI
    }

    val anthropicApiKey: String
        get() = System.getenv("KOOG_WORKSHOP_ANTHROPIC_API_KEY")
            ?: throw IllegalArgumentException("KOOG_WORKSHOP_OPENAI_API_KEY env is not set")

    val openAIApiKey: String
        get() = System.getenv("KOOG_WORKSHOP_OPENAI_API_KEY")
            ?: throw IllegalArgumentException("KOOG_WORKSHOP_OPENAI_API_KEY env is not set")

    val serviceProvider = ServiceProvider.ANTHROPIC

    val apiKey = when (serviceProvider) {
        ServiceProvider.ANTHROPIC -> anthropicApiKey
        ServiceProvider.OPENAI -> openAIApiKey
    }

    fun getDefaultClientAndModel(): Pair<LLMClient, LLModel> = when (serviceProvider) {
        ServiceProvider.OPENAI -> Pair(OpenAILLMClient(openAIApiKey), OpenAIModels.Chat.GPT4o)
        ServiceProvider.ANTHROPIC -> Pair(AnthropicLLMClient(anthropicApiKey), AnthropicModels.Sonnet_4)
    }
}