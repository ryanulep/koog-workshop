package com.jetbrains.koog.workshop.settings

object ApiKeyService {
    private const val KOOG_WORKSHOP_ANTHROPIC_API_KEY = "ANTHROPIC_API_KEY"

    enum class ServiceProvider {
        ANTHROPIC, OPENAI, GOOGLE
    }

    val serviceProvider = ServiceProvider.OPENAI

    val apiKey = when (serviceProvider) {
        ServiceProvider.ANTHROPIC -> anthropicApiKey
        ServiceProvider.OPENAI -> openAIApiKey
        ServiceProvider.GOOGLE -> geminiKey
    }

    val anthropicApiKey: String
        get() = System.getenv(KOOG_WORKSHOP_ANTHROPIC_API_KEY)
            ?: throw IllegalArgumentException("$KOOG_WORKSHOP_ANTHROPIC_API_KEY env is not set")

    val openAIApiKey: String
        get() = System.getenv("OPENAI_API_KEY")
            ?: throw IllegalArgumentException("OPENAI_API_KEY env is not set")

    val geminiKey: String
        get() = System.getenv("GEMINI_API_KEY")
            ?: throw IllegalArgumentException("GEMINI_API_KEY env is not set")
}