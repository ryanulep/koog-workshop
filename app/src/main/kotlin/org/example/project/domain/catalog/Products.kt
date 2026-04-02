package org.example.project.domain.catalog

import org.example.project.db.StoreTable
import org.example.project.domain.currency.Currencies
import org.jetbrains.exposed.v1.core.greaterEq

object Products : StoreTable("products") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val category = varchar("category", 50)          // ProductCategory.name
    val rarity = varchar("rarity", 50)              // Rarity.name
    val price = long("price").check("price_non_negative") { it greaterEq 0 }
    val currency = reference("currency_id", Currencies)
    val merchant = reference("merchant_id", Merchants)
    val stock = integer("stock").check("stock_non_negative") { it greaterEq 0 }.default(0)
    val imageUrl = varchar("image_url", 500).nullable()
    val isActive = bool("is_active").default(true)

    init {
        index(false, category)
        index(false, merchant)
        index(false, rarity)
    }
}
