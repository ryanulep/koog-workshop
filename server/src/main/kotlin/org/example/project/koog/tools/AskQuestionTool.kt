package org.example.project.koog.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

// FIXME similar to "intro", the class name is confusing since it doesn't define a tool, but a set of tools.
//  Rename to smth like "CommunicationTools" or convert to a class-based tool.
class AskQuestionTool(private val ask: suspend (message: String) -> Unit) : ToolSet {
    @Tool
    @LLMDescription("Ask a question to customer of the Fantasy Store assistant.")
    suspend fun askQuestion(message: String): String {
        ask(message) // message is send over SSE
        // TODO: Poll for response
        return "Can you check which orders I have pending? If not create a **ANY** new order for me."
    }
}