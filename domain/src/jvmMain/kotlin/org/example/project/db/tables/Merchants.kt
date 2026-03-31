package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Merchants : LongIdTable("merchants") {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val location = varchar("location", 255).nullable()
    val theme = varchar("theme", 100).nullable()
    val iconPath = varchar("icon_path", 500).nullable()
    val isActive = bool("is_active").default(true)
}
