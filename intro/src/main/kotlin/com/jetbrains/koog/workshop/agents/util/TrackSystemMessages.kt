package com.jetbrains.koog.workshop.agents.util

import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.message.Message

fun GraphAIAgent.FeatureContext.trackEvents(
    onToolCallEvent: suspend (String, Map<String, String>) -> Unit,
    onErrorEvent: suspend (String) -> Unit,
    onLLMCallEvent: suspend (List<Message>, List<ToolDescriptor>) -> Unit,
    onExecutionTraceEvent: suspend (AgentExecutionTraceEvent) -> Unit,
) {
    install(EventHandler) {
        onNodeExecutionStarting { ctx ->
            onExecutionTraceEvent(AgentExecutionTraceEvent.Node(ctx.node.name))
        }

        onSubgraphExecutionStarting { ctx ->
            onExecutionTraceEvent(AgentExecutionTraceEvent.SubgraphStarted(ctx.subgraph.name))
        }

        onSubgraphExecutionCompleted { ctx ->
            onExecutionTraceEvent(AgentExecutionTraceEvent.SubgraphCompleted(ctx.subgraph.name, result = ctx.output?.toString()))
        }

        onToolCallStarting { ctx ->
            onToolCallEvent(ctx.toolName, ctx.toolArgs.entries.mapValues { it.value.toString() })
        }

        onAgentExecutionFailed { ctx ->
            onErrorEvent("${ctx.error.message}")
        }

        onLLMCallStarting { ctx ->
            onLLMCallEvent(ctx.prompt.messages, ctx.tools)
        }
    }
}
