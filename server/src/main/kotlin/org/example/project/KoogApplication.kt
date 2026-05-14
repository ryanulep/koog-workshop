package org.example.project

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KoogApplication

fun main(args: Array<String>) {
    runApplication<KoogApplication>(*args)
}
