package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.Table

object ShippingMethods : LongIdTable("shipping_methods") {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val baseCost = long("base_cost")
    val currency = reference("currency_id", Currencies)
    val estimatedDays = integer("estimated_days")
    val isActive = bool("is_active").default(true)
}

object MerchantShippingMethods : Table("merchant_shipping_methods") {
    val merchant = reference("merchant_id", Merchants)
    val shippingMethod = reference("shipping_method_id", ShippingMethods)
    override val primaryKey = PrimaryKey(merchant, shippingMethod)
}
