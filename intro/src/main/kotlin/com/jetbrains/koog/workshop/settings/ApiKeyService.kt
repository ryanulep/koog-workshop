package com.jetbrains.koog.workshop.settings

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel

object ApiKeyService {
    enum class ServiceProvider {
        ANTHROPIC, OPENAI, GOOGLE
    }

    val serviceProvider = ServiceProvider.ANTHROPIC

    val apiKey = when (serviceProvider) {
        ServiceProvider.ANTHROPIC -> anthropicApiKey
        ServiceProvider.OPENAI -> openAIApiKey
        ServiceProvider.GOOGLE -> geminiKey
    }

    val anthropicApiKey: String
        get() = System.getenv("ANTHROPIC_API_KEY")
            ?: throw IllegalArgumentException("ANTHROPIC_API_KEY env is not set")

    val openAIApiKey: String
        get() = System.getenv("OPENAI_API_KEY")
            ?: throw IllegalArgumentException("OPENAI_API_KEY env is not set")

    val geminiKey: String
        get() = System.getenv("GEMINI_API_KEY")
            ?: throw IllegalArgumentException("GEMINI_API_KEY env is not set")

    fun getClientAndModel(): Pair<LLMClient, LLModel> = when (serviceProvider) {
        ServiceProvider.OPENAI -> Pair(OpenAILLMClient(openAIApiKey), OpenAIModels.Chat.GPT4o)
        ServiceProvider.ANTHROPIC -> Pair(AnthropicLLMClient(anthropicApiKey), AnthropicModels.Sonnet_4)
        ServiceProvider.GOOGLE -> Pair(GoogleLLMClient(geminiKey), GoogleModels.Gemini2_5FlashLite)
    }
}