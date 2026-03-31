package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Transactions : LongIdTable("transactions") {
    val character = reference("character_id", Characters)
    val currency = reference("currency_id", Currencies)
    val amount = long("amount")
    val type = varchar("type", 50)
    val referenceId = long("reference_id").nullable()
    val referenceType = varchar("reference_type", 50).nullable()
    val description = text("description").nullable()
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }

    init {
        index(false, character, currency)
    }
}
