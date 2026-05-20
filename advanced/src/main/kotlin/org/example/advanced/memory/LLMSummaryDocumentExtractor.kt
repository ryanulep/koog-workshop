package org.example.advanced.memory

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.longtermmemory.ingestion.extraction.DocumentExtractor
import ai.koog.agents.longtermmemory.model.MemoryRecord
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.xml.xml
import ai.koog.rag.base.TextDocument
import kotlinx.serialization.Serializable

class LLMSummaryDocumentExtractor(
    private val promptExecutor: PromptExecutor,
    private val model: LLModel = OpenAIModels.Chat.GPT5_5,
    private val prompt: Prompt = prompt("summarize-conversation") {
        system {
            +"Analyze the transcript of the user conversation and summarize new key facts to remember, if any."
            +"You must be concise and keep only new and essential information."
        }
    },
) : DocumentExtractor {
    override suspend fun extract(messages: List<Message>): List<TextDocument> {
        val conversationTranscript = messages
            .asSequence()
            .mapNotNull { message ->
                when (message) {
                    is Message.User -> message
                        .toText()
                        ?.let { text ->
                            xml {
                                tag("user") {
                                    +text
                                }
                            }
                        }

                    is Message.Assistant -> message
                        .toText()
                        ?.let { text ->
                            xml {
                                tag("assistant") {
                                    +text
                                }
                            }
                        }

                    // Skip system messages
                    is Message.System -> null
                }
            }
            .joinToString("\n")

        val updatedPrompt = prompt
            .withMessages { messages -> messages + Message.User(conversationTranscript, RequestMetaInfo.Empty) }

        return promptExecutor
            .executeStructured<ConversationFacts>(updatedPrompt, model)
            .getOrThrow().data.facts
            .map { MemoryRecord(it) }
    }

    private fun Message.toText(): String? {
        return parts
            .mapNotNull { part ->
                when (part) {
                    is MessagePart.Attachment -> null
                    is MessagePart.Reasoning -> null
                    is MessagePart.Text -> part.text
                    is MessagePart.Tool.Result -> part.output
                    is MessagePart.Tool.Call -> part.args
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")
    }
}

@Serializable
@LLMDescription("Analyzed and summarized conversation facts")
private data class ConversationFacts(
    @property:LLMDescription(
        "List of new key facts extracted from the conversation." +
            "Each list item should be a single fact." +
            "Leave empty if there were no new facts discovered."
    )
    val facts: List<String> = emptyList(),
    @property:LLMDescription("Optional free-form comments, defaults to empty string.")
    val comments: String = ""
)
