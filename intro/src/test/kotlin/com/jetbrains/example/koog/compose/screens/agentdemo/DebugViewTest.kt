package com.jetbrains.example.koog.compose.screens.agentdemo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugViewTest {

    private val baseVisible = listOf(
        ChatMessageType.User,
        ChatMessageType.Agent,
        ChatMessageType.System,
        ChatMessageType.Result,
    )

    @Test
    fun `disabled hides all debug messages regardless of options`() {
        val debugView = DebugView(enabled = false, options = DebugOption.entries.toSet())
        assertVisible(
            debugView = debugView,
            visible = baseVisible,
            hidden = listOf(
                ChatMessageType.Error,
                ChatMessageType.ToolCall,
                ChatMessageType.LlmCall,
                ChatMessageType.Node,
                ChatMessageType.Task,
            )
        )
    }

    @Test
    fun `enabled with no options shows only errors`() {
        val debugView = DebugView(enabled = true, options = emptySet())
        assertVisible(
            debugView = debugView,
            visible = baseVisible + ChatMessageType.Error,
            hidden = listOf(
                ChatMessageType.ToolCall,
                ChatMessageType.LlmCall,
                ChatMessageType.Node,
                ChatMessageType.Task,
            )
        )
    }

    @Test
    fun `enabled with all options shows everything`() {
        val debugView = DebugView(enabled = true, options = DebugOption.entries.toSet())
        ChatMessageType.entries.forEach { type ->
            assertTrue(debugView.shows(type), "Expected $type to be visible")
        }
    }

    @Test
    fun `tools option controls tool call visibility`() {
        val debugView = DebugView(enabled = true, options = setOf(DebugOption.Tools))
        assertTrue(debugView.shows(ChatMessageType.ToolCall))
        assertFalse(debugView.shows(ChatMessageType.LlmCall))
        assertFalse(debugView.shows(ChatMessageType.Node))
        assertFalse(debugView.shows(ChatMessageType.Task))
    }

    @Test
    fun `llm calls option controls llm call visibility`() {
        val debugView = DebugView(enabled = true, options = setOf(DebugOption.LlmCalls))
        assertFalse(debugView.shows(ChatMessageType.ToolCall))
        assertTrue(debugView.shows(ChatMessageType.LlmCall))
    }

    @Test
    fun `nodes option controls node visibility`() {
        val debugView = DebugView(enabled = true, options = setOf(DebugOption.Nodes))
        assertFalse(debugView.shows(ChatMessageType.ToolCall))
        assertTrue(debugView.shows(ChatMessageType.Node))
        assertFalse(debugView.shows(ChatMessageType.Task))
    }

    @Test
    fun `tasks option controls task visibility`() {
        val debugView = DebugView(enabled = true, options = setOf(DebugOption.Tasks))
        assertFalse(debugView.shows(ChatMessageType.ToolCall))
        assertFalse(debugView.shows(ChatMessageType.Node))
        assertTrue(debugView.shows(ChatMessageType.Task))
    }

    @Test
    fun `toggling master off and on preserves options`() {
        val original = DebugView(enabled = true, options = setOf(DebugOption.Tools, DebugOption.Nodes))
        val disabled = original.copy(enabled = false)
        val reEnabled = disabled.copy(enabled = true)
        assertEquals(original.options, reEnabled.options)
        assertTrue(reEnabled.shows(ChatMessageType.ToolCall))
        assertTrue(reEnabled.shows(ChatMessageType.Node))
        assertFalse(reEnabled.shows(ChatMessageType.LlmCall))
        assertFalse(reEnabled.shows(ChatMessageType.Task))
    }

    private fun assertVisible(
        debugView: DebugView,
        visible: List<ChatMessageType>,
        hidden: List<ChatMessageType>
    ) {
        visible.forEach { type ->
            assertTrue(debugView.shows(type), "Expected $type to be visible for $debugView")
        }
        hidden.forEach { type ->
            assertFalse(debugView.shows(type), "Expected $type to be hidden for $debugView")
        }
    }
}
