package org.example.project

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fantasy Store Admin",
        state = rememberWindowState(
            size = DpSize(1500.dp, 800.dp)
        )
    ) {
        App()
    }
}
