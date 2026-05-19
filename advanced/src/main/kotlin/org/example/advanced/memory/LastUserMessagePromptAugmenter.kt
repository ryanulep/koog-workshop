package org.example.advanced.memory

import ai.koog.agents.longtermmemory.retrieval.augmentation.PromptAugmenter
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.rag.base.TextDocument
import ai.koog.rag.base.storage.search.SearchResult

/**
 * Augments the last user message in the prompt with additional text part with relevant context, if found.
 */
class LastUserMessagePromptAugmenter(
    private val template: String = DEFAULT_USER_PROMPT_TEMPLATE,
    private val contextPrefix: String = PromptAugmenter.DEFAULT_CONTEXT_PREFIX
) : PromptAugmenter {

    /**
     * Companion object with default templates.
     */
    companion object {
        /**
         * Default template for user context insertion.
         * Use [PromptAugmenter.RELEVANT_CONTEXT_PLACEHOLDER] placeholder.
         */
        val DEFAULT_USER_PROMPT_TEMPLATE: String =
            """
            |Here is some relevant context:
            |
            |${PromptAugmenter.RELEVANT_CONTEXT_PLACEHOLDER}
            |
            |Based on the above context, please answer the following question:
            """.trimMargin().trim()
    }


    override fun augment(originalPrompt: Prompt, relevantContext: List<SearchResult<TextDocument>>): Prompt {
        if (relevantContext.isEmpty()) return originalPrompt
        if (originalPrompt.messages.none { it is Message.User }) return originalPrompt

        val relevantContextText = PromptAugmenter.formatContext(relevantContext, contextPrefix)
        val formattedContext = PromptAugmenter.formatTemplate(template, relevantContextText)
        if (formattedContext.isBlank()) return originalPrompt

        return originalPrompt.withMessages { messages ->
            val lastUserIndex = messages.indexOfLast { it is Message.User }

            if (lastUserIndex >= 0) {
                val updatedMessage = (messages[lastUserIndex] as Message.User)
                    .let { message -> message.copy(parts = message.parts + MessagePart.Text(formattedContext)) }

                messages
                    .toMutableList()
                    .also { it[lastUserIndex] = updatedMessage }
                    .toList()
            } else {
                messages
            }
        }
    }
}