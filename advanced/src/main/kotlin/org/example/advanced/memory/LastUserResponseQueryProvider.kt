package org.example.advanced.memory

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.longtermmemory.retrieval.search.SearchQueryProvider
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart

/**
 * Returns the last user message or/and response from the [askTool] as a query, because [askTool] should also be treated
 * as "user messages"
 */
class LastUserResponseQueryProvider(
    private val askTool: Tool<*, *>,
) : SearchQueryProvider {
    override fun provide(prompt: Prompt): String? {
        return prompt.messages
            .asSequence()
            .filterIsInstance<Message.User>()
            .mapNotNull { message ->
                val text = message.parts
                    .filterIsInstance<MessagePart.Text>()
                    .joinToString("\n") { it.text }
                    .takeIf { it.isNotBlank() }

                val askToolOutput = message.parts
                    .filterIsInstance<MessagePart.Tool.Result>()
                    .filter { it.tool == askTool.name }
                    .joinToString("\n") { it.output }
                    .takeIf { it.isNotBlank() }

                listOfNotNull(text, askToolOutput)
                    .joinToString("\n\n")
                    .takeIf { it.isNotBlank() }
            }
            .lastOrNull()
    }
}
