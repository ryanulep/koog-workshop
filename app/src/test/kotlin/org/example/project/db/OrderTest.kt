package org.example.project.db

import org.example.project.domain.character.*
import org.example.project.domain.catalog.*
import org.example.project.domain.currency.*
import org.example.project.domain.order.*
import org.example.project.domain.shipping.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class OrderTest {

    @BeforeTest
    fun setup() {
        connectSqlite("jdbc:sqlite:file:test_orders?mode=memory&cache=shared")
    }

    @Test
    fun testSchemaCreation() {
        transaction {
            SchemaUtils.create(
                Characters, Currencies, Merchants, Products, ShippingMethods, 
                Orders, SubOrders, OrderItems
            )
        }
    }

    @Test
    fun testCreateOrderHierarchy() {
        transaction {
            SchemaUtils.create(
                Characters, Currencies, Merchants, Products, ShippingMethods, 
                Orders, SubOrders, OrderItems
            )

            val charId = Characters.insertAndGetId { it[name] = "Hero" }
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Merchant" }
            val prodId = Products.insertAndGetId {
                it[Products.name] = "Excalibur"
                it[Products.category] = "WEAPONS"
                it[Products.rarity] = "LEGENDARY"
                it[Products.price] = 1000
                it[Products.currency] = goldId
                it[Products.merchant] = merchantId
            }
            val shipId = ShippingMethods.insertAndGetId {
                it[ShippingMethods.name] = "Teleportation"
                it[ShippingMethods.baseCost] = 100
                it[ShippingMethods.currency] = goldId
                it[ShippingMethods.estimatedDays] = 0
            }

            // 1. Parent Order
            val orderId = Orders.insertAndGetId {
                it[Orders.character] = charId
                it[Orders.status] = OrderStatus.PENDING.name
                it[Orders.totalPrice] = 1100
                it[Orders.totalCurrency] = goldId
            }

            // 2. SubOrder
            val subOrderId = SubOrders.insertAndGetId {
                it[SubOrders.order] = orderId
                it[SubOrders.merchant] = merchantId
                it[SubOrders.status] = OrderStatus.PENDING.name
                it[SubOrders.shippingMethod] = shipId
                it[SubOrders.shippingCost] = 100
                it[SubOrders.merchantTotalPrice] = 1100
            }

            // 3. OrderItem
            val orderItemId = OrderItems.insertAndGetId {
                it[OrderItems.subOrder] = subOrderId
                it[OrderItems.product] = prodId
                it[OrderItems.quantity] = 1
                it[OrderItems.snapshottedPrice] = 1000
                it[OrderItems.snapshottedCurrency] = goldId
            }

            // Verification
            val order = Orders.selectAll().where { Orders.id eq orderId }.single()
            assertEquals(charId, order[Orders.character])
            assertEquals(OrderStatus.PENDING.name, order[Orders.status])

            val subOrder = SubOrders.selectAll().where { SubOrders.id eq subOrderId }.single()
            assertEquals(orderId, subOrder[SubOrders.order])
            assertEquals(merchantId, subOrder[SubOrders.merchant])

            val orderItem = OrderItems.selectAll().where { OrderItems.id eq orderItemId }.single()
            assertEquals(subOrderId, orderItem[OrderItems.subOrder])
            assertEquals(prodId, orderItem[OrderItems.product])
            assertEquals(1, orderItem[OrderItems.quantity])
        }
    }
}
