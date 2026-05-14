package com.jetbrains.koog.workshop.agents.weather

import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.prompt.message.Message

/*
Low-level implementation of the basic loop strategy by hand.
 */
fun basicSingleRunStrategyByHand() = strategy("single-run-strategy-by-hand") {
    val nodeCallLLM: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest()
    val nodeExecuteTool: AIAgentNodeBase<Message.Tool.Call, ReceivedToolResult> by nodeExecuteTool()
    val nodeSendToolResult: AIAgentNodeBase<ReceivedToolResult, Message.Response> by nodeLLMSendToolResult()

    edge(nodeStart forwardTo nodeCallLLM)
    edge(nodeCallLLM forwardTo nodeFinish onAssistantMessage { true })

    // Task: Complete the strategy implementation.
    edge(nodeCallLLM forwardTo nodeExecuteTool onToolCall { true })
    edge(nodeExecuteTool forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })
}
