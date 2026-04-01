@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project

import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.catalog.DamageType
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Product
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.catalog.WeaponSlot
import org.example.project.domain.catalog.Weapons
import org.example.project.domain.character.Characters
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.Order
import org.example.project.domain.order.OrderItem
import org.example.project.domain.order.OrderItems
import org.example.project.domain.order.OrderRepository
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.Orders
import org.example.project.domain.order.SubOrder
import org.example.project.domain.order.SubOrders
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.SubOrderId
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertNotNull
import kotlin.time.Instant

internal data class ProductMutationScenario(
    val database: Database,
    val productId: ProductId,
    val productName: String,
    val merchantName: String,
    val initialStock: Int
) {
    val rowDescription: String
        get() = "Product $productName from $merchantName"

    fun rowState(stock: Int = initialStock, isActive: Boolean = true): String =
        "${if (isActive) "Active" else "Inactive"}, stock $stock"

    fun loadProduct(): Product =
        transaction(database) {
            assertNotNull(ProductRepository().getProductOrNull(productId))
        }
}

internal data class OrderMutationScenario(
    val database: Database,
    val orderId: OrderId,
    val subOrderId: SubOrderId,
    val characterName: String,
    val merchantName: String,
    val currencyCode: String,
    val totalPrice: Long,
    val merchantCount: Int
) {
    val orderRowDescription: String
        get() = "Order ${orderId.value} for $characterName"

    fun orderRowState(status: OrderStatus): String =
        "${status.labelize()}, $merchantCount merchants, $totalPrice $currencyCode"

    val subOrderDescription: String
        get() = "Sub-order ${subOrderId.value} for $merchantName"

    fun loadOrder(): Order =
        transaction(database) {
            assertNotNull(OrderRepository().getOrderOrNull(orderId))
        }

    fun loadSubOrder(): SubOrder =
        transaction(database) {
            assertNotNull(OrderRepository().getSubOrderOrNull(subOrderId))
        }
}

internal fun createProductMutationScenario(): ProductMutationScenario {
    val token = UniqueAdminFixtureToken.next("product")
    val database = createIsolatedDatabase("compose_product")

    return transaction(database) {
        val currencyCode = token.currencyCode("GLD")
        val currencyId = Currencies.insertAndGetId {
            it[code] = currencyCode
            it[name] = "Gold $token"
            it[symbol] = "G"
        }
        val merchantName = "Blackforge Armory $token"
        val merchantId = Merchants.insertAndGetId {
            it[name] = merchantName
            it[description] = "Forged weapons for $token"
            it[location] = "North Ward"
        }

        val productName = "Bronze Blade $token"
        val initialStock = 12
        val productId = Products.insertAndGetId {
            it[name] = productName
            it[description] = "A dependable forged short sword for $token"
            it[category] = ProductCategory.WEAPONS.name
            it[rarity] = Rarity.UNCOMMON.name
            it[price] = 320
            it[currency] = currencyId
            it[merchant] = merchantId
            it[stock] = initialStock
            it[createdAt] = Instant.fromEpochMilliseconds(2_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
        }
        Weapons.insert {
            it[id] = productId
            it[damage] = 14
            it[damageType] = DamageType.PHYSICAL.name
            it[weaponSlot] = WeaponSlot.MAIN_HAND.name
        }

        ProductMutationScenario(
            database = database,
            productId = ProductId(productId.value),
            productName = productName,
            merchantName = merchantName,
            initialStock = initialStock
        )
    }
}

internal fun createOrderMutationScenario(): OrderMutationScenario {
    val token = UniqueAdminFixtureToken.next("order")
    val database = createIsolatedDatabase("compose_order")

    return transaction(database) {
        val currencyCode = token.currencyCode("SLV")
        val currencyId = Currencies.insertAndGetId {
            it[code] = currencyCode
            it[name] = "Silver $token"
            it[symbol] = "S"
        }
        val merchantName = "Courier Quartermaster $token"
        val merchantId = Merchants.insertAndGetId {
            it[name] = merchantName
            it[description] = "Coordinates fulfilment for $token"
            it[location] = "Harbor Gate"
        }
        val characterName = "Aldric $token"
        val characterId = Characters.insertAndGetId {
            it[name] = characterName
        }
        val shippingMethodId = ShippingMethods.insertAndGetId {
            it[name] = "Skyship Express $token"
            it[description] = "Fast delivery lane for $token"
            it[baseCost] = 25
            it[currency] = currencyId
            it[estimatedDays] = 2
        }
        val productId = Products.insertAndGetId {
            it[name] = "Crate of Rations $token"
            it[description] = "Packed for expedition $token"
            it[category] = ProductCategory.MISCELLANEOUS.name
            it[rarity] = Rarity.COMMON.name
            it[price] = 320
            it[currency] = currencyId
            it[merchant] = merchantId
            it[stock] = 8
            it[createdAt] = Instant.fromEpochMilliseconds(3_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(3_000)
        }
        val totalPrice = 320L
        val orderId = Orders.insertAndGetId {
            it[character] = characterId
            it[status] = OrderStatus.PENDING.name
            it[Orders.totalPrice] = totalPrice
            it[totalCurrency] = currencyId
            it[createdAt] = Instant.fromEpochMilliseconds(4_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(4_000)
        }
        val subOrderId = SubOrders.insertAndGetId {
            it[order] = orderId
            it[merchant] = merchantId
            it[status] = OrderStatus.PENDING.name
            it[shippingMethod] = shippingMethodId
            it[shippingCost] = 25
            it[merchantTotalPrice] = 345
            it[createdAt] = Instant.fromEpochMilliseconds(4_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(4_000)
        }
        OrderItems.insertAndGetId {
            it[subOrder] = subOrderId
            it[product] = productId
            it[quantity] = 1
            it[snapshottedPrice] = 320
            it[snapshottedCurrency] = currencyId
            it[createdAt] = Instant.fromEpochMilliseconds(4_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(4_000)
        }

        OrderMutationScenario(
            database = database,
            orderId = OrderId(orderId.value),
            subOrderId = SubOrderId(subOrderId.value),
            characterName = characterName,
            merchantName = merchantName,
            currencyCode = currencyCode,
            totalPrice = totalPrice,
            merchantCount = 1
        )
    }
}

internal fun stockAdjustmentAccessibilityDescription(quantityChange: Int): String =
    if (quantityChange >= 0) {
        "Increase stock by $quantityChange"
    } else {
        "Decrease stock by ${-quantityChange}"
    }

internal fun orderStatusAccessibilityDescription(status: OrderStatus): String =
    "Set order status to ${status.labelize()}"

internal fun subOrderStatusAccessibilityDescription(
    subOrderId: SubOrderId,
    status: OrderStatus
): String = "Set sub-order ${subOrderId.value} status to ${status.labelize()}"

private fun createIsolatedDatabase(prefix: String): Database {
    val databaseFile = File.createTempFile("${prefix}_", ".db").apply {
        deleteOnExit()
    }
    return connectSqlite(databaseFile).createTables()
}

private object UniqueAdminFixtureToken {
    private val sequence = AtomicLong(0)

    fun next(prefix: String): String {
        val id = sequence.incrementAndGet().toString(36)
        val nanos = System.nanoTime().toString(36)
        return "$prefix-$id-$nanos"
    }
}

private fun String.currencyCode(prefix: String): String =
    (prefix + filter(Char::isLetterOrDigit).uppercase()).take(20)

private fun OrderStatus.labelize(): String =
    name.lowercase()
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { segment ->
            segment.replaceFirstChar { char ->
                if (char.isLowerCase()) {
                    char.titlecase()
                } else {
                    char.toString()
                }
            }
        }
