package com.jetbrains.example.koog.compose.agents.util

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

// FIXME Class name sounds confusing, because it's not the tool itself, but a tool set.
//  Either rename it to smth like "CommunicationTools" or convert to a class-based tool
class AskUserTool(private val ask: suspend (message: String) -> String) : ToolSet {
    @Tool
    @LLMDescription("Ask the user a question and wait for their response. Use this to gather information from the user.")
    suspend fun askUser(question: String): String = ask(question)
}