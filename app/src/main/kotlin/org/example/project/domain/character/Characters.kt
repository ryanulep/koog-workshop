package org.example.project.domain.character

import org.example.project.db.StoreTable

object Characters : StoreTable("characters") {
    val name = varchar("name", 255).uniqueIndex()
}
