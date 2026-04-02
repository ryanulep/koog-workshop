package org.example.project.domain.catalog

import org.example.project.db.StoreTable

object Merchants : StoreTable("merchants") {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val location = varchar("location", 255).nullable()
    val theme = varchar("theme", 100).nullable()
    val iconPath = varchar("icon_path", 500).nullable()
    val isActive = bool("is_active").default(true)
}
