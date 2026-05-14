package org.example.project.koog.tools

import ai.koog.agents.core.tools.reflect.ToolFromCallable
import ai.koog.agents.core.tools.reflect.ToolSet

operator fun ToolSet.plus(other: ToolSet): List<ToolFromCallable<*>> = asTools() + other.asTools()
