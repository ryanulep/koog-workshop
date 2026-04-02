package org.example.project.koog

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

class AskQuestionTool(private val ask: suspend (message: String) -> String) : ToolSet {
    @Tool
    @LLMDescription("Ask a question to customer of the Fantasy Store assistant.")
    suspend fun askQuestion(message: String): String = ask(message)
}