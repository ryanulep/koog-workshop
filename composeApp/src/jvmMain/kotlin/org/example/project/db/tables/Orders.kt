package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Orders : LongIdTable("orders") {
    val character = reference("character_id", Characters)
    val status = varchar("status", 50)              // OrderStatus.name
    val totalPrice = long("total_price")
    val totalCurrency = reference("total_currency_id", Currencies)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val updatedAt = long("updated_at").clientDefault { System.currentTimeMillis() }
}

object SubOrders : LongIdTable("sub_orders") {
    val order = reference("order_id", Orders)
    val merchant = reference("merchant_id", Merchants)
    val status = varchar("status", 50)              // OrderStatus.name
    val shippingMethod = reference("shipping_method_id", ShippingMethods)
    val shippingCost = long("shipping_cost")
    val merchantTotalPrice = long("merchant_total_price")
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val updatedAt = long("updated_at").clientDefault { System.currentTimeMillis() }
}

object OrderItems : LongIdTable("order_items") {
    val subOrder = reference("sub_order_id", SubOrders)
    val product = reference("product_id", Products)
    val quantity = integer("quantity")
    val snapshottedPrice = long("snapshotted_price")
    val snapshottedCurrency = reference("snapshotted_currency_id", Currencies)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
}
