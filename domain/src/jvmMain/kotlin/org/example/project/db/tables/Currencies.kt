package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Currencies : LongIdTable("currencies") {
    val code = varchar("code", 50).uniqueIndex()
    val name = varchar("name", 255)
    val symbol = varchar("symbol", 10)
    val iconPath = varchar("icon_path", 500).nullable()
}
