package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Characters : LongIdTable("characters") {
    val name = varchar("name", 255).uniqueIndex()
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
}
