package com.jetbrains.koog.workshop.agents.weather

import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.prompt.message.Message

/**
 * Low-level implementation of the basic loop strategy by hand.
 */
fun basicSingleRunStrategyByHand() = strategy<String, String>("single-run-strategy-by-hand") {
    val nodeCallLLM: AIAgentNodeBase<Message.User, Message.Assistant> by nodeLLMSendMessage()
    val nodeExecuteTools: AIAgentNodeBase<ToolCalls, ReceivedToolResults> by nodeExecuteTools()

    // TODO: Replace the following with the default strategy implementation:
    edge(nodeStart forwardTo nodeFinish)
}