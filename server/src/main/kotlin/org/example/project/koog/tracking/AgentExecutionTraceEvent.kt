package org.example.project.koog.tracking

sealed interface AgentExecutionTraceEvent {
    val name: String

    data class Node(override val name: String) : AgentExecutionTraceEvent
    data class Subgraph(override val name: String) : AgentExecutionTraceEvent
}