package com.jetbrains.koog.workshop.agents.util

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet

class CommunicationTools(
    private val ask: suspend (message: String) -> String
) : ToolSet {

    @Tool
    @LLMDescription("Ask the user a question and wait for their response. Use this to gather information from the user.")
    suspend fun askUser(question: String): String = ask(question)
}