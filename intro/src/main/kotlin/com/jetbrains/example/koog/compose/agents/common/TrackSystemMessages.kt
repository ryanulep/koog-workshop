package com.jetbrains.example.koog.compose.agents.common

import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.message.Message

fun GraphAIAgent.FeatureContext.trackEvents(
    onToolCallEvent: suspend (String, Map<String, String>) -> Unit,
    onErrorEvent: suspend (String) -> Unit,
    onLLMCallEvent: suspend (List<Message>, List<ToolDescriptor>) -> Unit,
    onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
) {
    handleEvents {
        onNodeExecutionStarting { ctx ->
            onExecutionTraceEvent(AgentExecutionTraceEvent.Node(ctx.node.name))
        }

        onSubgraphExecutionStarting { ctx ->
            onExecutionTraceEvent(AgentExecutionTraceEvent.Subgraph(ctx.subgraph.name))
        }

        onToolCallStarting { ctx ->
            onToolCallEvent(ctx.toolName, ctx.toolArgs.entries.mapValues { it.value.toString() })
        }

        onAgentExecutionFailed { ctx ->
            onErrorEvent("${ctx.throwable.message}")
        }

        onLLMCallStarting { ctx ->
            onLLMCallEvent(ctx.prompt.messages, ctx.tools)
        }
    }
}
