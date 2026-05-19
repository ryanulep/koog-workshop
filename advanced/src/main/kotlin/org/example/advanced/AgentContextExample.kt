package org.example.advanced

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.context.DetachedPromptExecutorAPI
import ai.koog.agents.core.agent.context.agentInput
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.MessagePart
import kotlinx.serialization.Serializable

@Serializable
data class UserQueryMetadata(
    val originalQuery: String,
    val wordCount: Int,
    val containsQuestion: Boolean,
)

suspend fun main() {
    val openaiApiKey = System.getenv("OPENAI_API_KEY")

    MultiLLMPromptExecutor(
        LLMProvider.OpenAI to OpenAILLMClient(openaiApiKey)
    ).use { promptExecutor ->
        runAgent(promptExecutor)
    }
}

private suspend fun runAgent(promptExecutor: MultiLLMPromptExecutor) {
    val strategy = strategy<String, String>("context-demo") {
        val queryKey = createStorageKey<String>("original-query")
        val metadataKey = createStorageKey<UserQueryMetadata>("query-metadata")

        /*
         * 1. agentInput<T>()
         * context.agentInput: Any? holds the typed input passed to the current
         * node. agentInput<T>() casts it to T - it equals the lambda param here,
         */
        val nodeShowAgentInput by node<String, String> { input ->
            val query = agentInput<String>()  // same value as `input`
            check(query == input)
            println("[agentInput] \"$query\"")
            storage.set(queryKey, query)
            query
        }

        /*
         * 2. storage
         * AIAgentStorage is a concurrent-safe key-value store shared across all
         * nodes of a single agent run. Keys are created with createStorageKey<T>()
         * Use getValue() to assert presence or get() to allow null.
         * If the value is serializable with JSONSerializer configured in the AIAgentConfig.serializer,
         * it will also be automatically persisted when Persistency feature is installed.
         */
        val nodeUseStorage by node<String, String> { _ ->
            val query = storage.getValue(queryKey)
            val metadata = UserQueryMetadata(
                originalQuery = query,
                wordCount = query.split(Regex("\\s+")).size,
                containsQuestion = query.trimEnd().endsWith('?'),
            )
            storage.set(metadataKey, metadata)
            println("[storage] $metadata")
            query
        }

        /*
         * 3. llm.writeSession { }
         * writeSession acquires an exclusive write lock on the LLM context for
         * the duration of the block. Inside you can, among other things:
         *   appendPrompt { … }  - add messages; changes are committed on block exit
         *   prompt = newPrompt  - completely overwrite the prompt e.g. to change message history or LLM parameters
         *   model = newModel    - swap the active model for this and later nodes
         *   requestLLM()        - call the LLM and append the response to the prompt
         *   callTool()          - call the tool manually, providing the arguments
         * All mutations are committed atomically when the block returns normally.
         * Do NOT nest another session on the same context - the lock is not reentrant (at least for now).
         */
        val nodeWriteSession by node<String, String> { query ->
            val meta = storage.getValue(metadataKey)
            llm.writeSession {
                appendPrompt {
                    user(
                        "Query: \"$query\". Word count: ${meta.wordCount}. " +
                        "Is a question: ${meta.containsQuestion}. Reply in one sentence."
                    )
                }
                // requestLLM sends the full current prompt and appends the assistant reply
                val response = requestLLM()
                response.parts.filterIsInstance<MessagePart.Text>().joinToString("") { it.text }
            }
        }

        /*
         * 4. llm.readSession { }
         * readSession acquires a shared read lock - multiple nodes can read
         * concurrently. Inside the block you receive a consistent snapshot of
         * prompt, tools, and model. Mutations are not persisted back to the context.
         * requestLLM() works here too, but the response is NOT appended to the
         * agent's conversation history - it is ephemeral. Use readSession for
         * inspection, validation, or side-channel LLM calls that must not alter
         * the running conversation.
         */
        val nodeReadSession by node<String, String> { llmReply ->
            llm.readSession {
                val msgCount = prompt.messages.size
                val toolNames = tools.map { it.name }
                println("[readSession] messages in prompt: $msgCount, tools: $toolNames")

                // Calling requestLLM() here is valid (it sends the current prompt)
                // but the response is NOT appended to the agent's history:
                // val peek = requestLLM()  // ephemeral - does not persist
            }
            llmReply
        }

        /*
         * 5. llm.promptExecutor - detached API
         * promptExecutor exposes the raw PromptExecutor behind the agent.
         * Calls here are completely disconnected from the agent lifecycle:
         *   • the request and response are NOT added to the agent's prompt history
         *   • ToolsConversionStrategy is NOT applied
         *   • agent observability hooks do NOT fire
         * Requires @OptIn(DetachedPromptExecutorAPI::class).
         * Useful for one-shot side calls: moderation, classification, etc.
         */
        val nodeDetachedLLM by node<String, String> { llmReply ->
            @OptIn(DetachedPromptExecutorAPI::class)
            val response = llm.promptExecutor.execute(
                prompt("sentiment-check") {
                    system("Classify the sentiment of the text. Reply with a single word: POSITIVE, NEGATIVE, or NEUTRAL.")
                    user(llmReply)
                },
                OpenAIModels.Chat.GPT4oMini,
            )
            val sentiment = response.parts
                .filterIsInstance<MessagePart.Text>()
                .joinToString("") { it.text }
                .trim()
                .ifEmpty { "UNKNOWN" }
            println("[promptExecutor] detached sentiment check: $sentiment")
            "$llmReply [sentiment: $sentiment]"
        }

        nodeStart then nodeShowAgentInput
        nodeShowAgentInput then nodeUseStorage
        nodeUseStorage then nodeWriteSession
        nodeWriteSession then nodeReadSession
        nodeReadSession then nodeDetachedLLM
        nodeDetachedLLM then nodeFinish
    }

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = strategy,
        agentConfig = AIAgentConfig(
            prompt = prompt("example") {
                system { +"You're a helpful assistant." }
            },
            model = OpenAIModels.Chat.GPT4oMini,
            maxAgentIterations = 50,
        ),
    )

    val result = agent.run("What is the capital of France?")
    println("Agent result: $result")
}
