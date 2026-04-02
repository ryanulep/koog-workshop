package org.example.project.domain.order

import org.example.project.db.StoreTable
import org.example.project.domain.character.Characters
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Products
import org.example.project.domain.currency.Currencies
import org.example.project.domain.shipping.ShippingMethods
import org.jetbrains.exposed.v1.core.greaterEq

object Orders : StoreTable("orders") {
    val character = reference("character_id", Characters)
    val status = varchar("status", 50)              // OrderStatus.name
    val totalPrice = long("total_price").check("total_price_non_negative") { it greaterEq 0 }
    val totalCurrency = reference("total_currency_id", Currencies)
}

object SubOrders : StoreTable("sub_orders") {
    val order = reference("order_id", Orders)
    val merchant = reference("merchant_id", Merchants)
    val status = varchar("status", 50)              // OrderStatus.name
    val shippingMethod = reference("shipping_method_id", ShippingMethods)
    val shippingCost = long("shipping_cost").check("shipping_cost_non_negative") { it greaterEq 0 }
    val merchantTotalPrice = long("merchant_total_price").check("merchant_total_price_non_negative") { it greaterEq 0 }
}

object OrderItems : StoreTable("order_items") {
    val subOrder = reference("sub_order_id", SubOrders)
    val product = reference("product_id", Products)
    val quantity = integer("quantity").check("quantity_positive") { it greaterEq 1 }
    val snapshottedPrice = long("snapshotted_price").check("snapshotted_price_non_negative") { it greaterEq 0 }
    val snapshottedCurrency = reference("snapshotted_currency_id", Currencies)
}
