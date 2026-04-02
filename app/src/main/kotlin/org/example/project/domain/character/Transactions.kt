package org.example.project.domain.character

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters
import org.example.project.domain.currency.Currencies

object Transactions : StoreTable("transactions") {
    val character = reference("character_id", Characters)
    val currency = reference("currency_id", Currencies)
    val amount = long("amount")
    val type = varchar("type", 50)
    val referenceId = uuid("reference_id").nullable()
    val referenceType = varchar("reference_type", 50).nullable()
    val description = text("description").nullable()

    init {
        index(false, character, currency)
    }
}
