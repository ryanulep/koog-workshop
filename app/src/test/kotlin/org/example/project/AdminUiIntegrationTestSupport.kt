@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project

import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.db.update as storeUpdate
import org.example.project.domain.catalog.DamageType
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Potions
import org.example.project.domain.catalog.Product
import org.example.project.domain.catalog.ProductCategory
import org.example.project.domain.catalog.ProductRepository
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Rarity
import org.example.project.domain.catalog.Scrolls
import org.example.project.domain.catalog.WeaponSlot
import org.example.project.domain.catalog.Weapons
import org.example.project.domain.character.Characters
import org.example.project.domain.character.TransactionType
import org.example.project.domain.character.Transactions
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.Order
import org.example.project.domain.order.OrderItems
import org.example.project.domain.order.OrderRepository
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.Orders
import org.example.project.domain.order.SubOrder
import org.example.project.domain.order.SubOrders
import org.example.project.domain.review.Reviews
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.shared.OrderId
import org.example.project.domain.shared.ProductId
import org.example.project.domain.shared.SubOrderId
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
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

internal data class AdminProductFixture(
    val id: ProductId,
    val name: String,
    val merchantName: String,
    val description: String,
    val category: ProductCategory,
    val price: Long,
    val currencyCode: String,
    val stock: Int,
    val isActive: Boolean,
    val reviewText: String,
    val categoryFieldLabel: String,
    val categoryFieldValue: String
) {
    val rowDescription: String
        get() = "Product $name from $merchantName"

    fun rowState(stock: Int = this.stock, isActive: Boolean = this.isActive): String =
        "${if (isActive) "Active" else "Inactive"}, stock $stock"

    fun activationButtonText(isActive: Boolean = this.isActive): String =
        if (isActive) "Deactivate product" else "Activate product"

    val priceText: String
        get() = "$price $currencyCode"
}

internal data class AdminSubOrderFixture(
    val id: SubOrderId,
    val merchantName: String,
    val initialStatus: OrderStatus
) {
    val description: String
        get() = "Sub-order ${id.value} for $merchantName"

    fun state(status: OrderStatus = initialStatus): String = status.labelize()
}

internal data class AdminOrderFixture(
    val orderId: OrderId,
    val characterName: String,
    val initialStatus: OrderStatus,
    val merchantCount: Int,
    val totalPrice: Long,
    val currencyCode: String,
    val itemNames: List<String>,
    val historyDescription: String
) {
    val rowDescription: String
        get() = "Order ${orderId.value} for $characterName"

    fun rowState(status: OrderStatus = initialStatus): String =
        "${status.labelize()}, $merchantCount merchants, $totalPrice $currencyCode"

    val title: String
        get() = "Order ${orderId.value}"
}

internal data class AdminAppScenario(
    val database: Database,
    val primaryProduct: AdminProductFixture,
    val inactiveProduct: AdminProductFixture,
    val extraProduct: AdminProductFixture,
    val newestOrder: AdminOrderFixture,
    val olderOrder: AdminOrderFixture,
    val mutableSubOrder: AdminSubOrderFixture
) {
    val products: List<AdminProductFixture>
        get() = listOf(primaryProduct, inactiveProduct, extraProduct)

    val orders: List<AdminOrderFixture>
        get() = listOf(newestOrder, olderOrder)

    val refreshedProductName: String = "Tempered Aether Blade"
    val refreshedProductDescription: String = "Rebalanced steel for veteran wardens."
    val refreshedProductStock: Int = 19

    val refreshedOrderStatus: OrderStatus = OrderStatus.CANCELLED
    val refreshedSubOrderStatus: OrderStatus = OrderStatus.DELIVERED
    val refreshedOrderHistoryDescription: String = "Order manually reviewed."

    val refreshedPrimaryProduct: AdminProductFixture
        get() = primaryProduct.copy(
            name = refreshedProductName,
            description = refreshedProductDescription,
            stock = refreshedProductStock
        )

    fun refreshedNewestOrder(status: OrderStatus = refreshedOrderStatus): AdminOrderFixture =
        newestOrder.copy(initialStatus = status)

    fun refreshedMutableSubOrder(status: OrderStatus = refreshedSubOrderStatus): AdminSubOrderFixture =
        mutableSubOrder.copy(initialStatus = status)

    fun loadProduct(productId: ProductId = primaryProduct.id): Product =
        transaction(database) {
            assertNotNull(ProductRepository().getProductOrNull(productId))
        }

    fun loadOrder(orderId: OrderId = newestOrder.orderId): Order =
        transaction(database) {
            assertNotNull(OrderRepository().getOrderOrNull(orderId))
        }

    fun loadSubOrder(subOrderId: SubOrderId = mutableSubOrder.id): SubOrder =
        transaction(database) {
            assertNotNull(OrderRepository().getSubOrderOrNull(subOrderId))
        }

    fun refreshProductFromDatabase() {
        transaction(database) {
            Products.storeUpdate(primaryProduct.id.value) {
                it[name] = refreshedProductName
                it[description] = refreshedProductDescription
                it[stock] = refreshedProductStock
            }
        }
    }

    fun refreshOrderFromDatabase() {
        transaction(database) {
            Orders.storeUpdate(newestOrder.orderId.value) {
                it[status] = refreshedOrderStatus.name
            }
            SubOrders.storeUpdate(mutableSubOrder.id.value) {
                it[status] = refreshedSubOrderStatus.name
            }
            Transactions.insert {
                it[character] = Characters.selectAll()
                    .where { Characters.name eq newestOrder.characterName }
                    .single()[Characters.id]
                it[currency] = Currencies.selectAll().single()[Currencies.id]
                it[amount] = -newestOrder.totalPrice
                it[type] = TransactionType.PURCHASE.name
                it[referenceId] = newestOrder.orderId.value
                it[referenceType] = "ORDER"
                it[description] = refreshedOrderHistoryDescription
                it[createdAt] = Instant.fromEpochMilliseconds(6_100)
                it[updatedAt] = Instant.fromEpochMilliseconds(6_100)
            }
        }
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

internal fun createAdminAppScenario(): AdminAppScenario {
    val database = createIsolatedDatabase("compose_admin_app")

    return transaction(database) {
        val currencyCode = "GOLD"
        val currencyId = Currencies.insertAndGetId {
            it[code] = currencyCode
            it[name] = "Gold"
            it[symbol] = "G"
        }

        val blackforgeId = Merchants.insertAndGetId {
            it[name] = "Blackforge Armory"
            it[description] = "Forged steel and field-ready armor."
            it[location] = "North Ward"
        }
        val moonwellId = Merchants.insertAndGetId {
            it[name] = "Moonwell Remedies"
            it[description] = "Alchemical draughts and restorative tonics."
            it[location] = "Canal Market"
        }
        val starfallId = Merchants.insertAndGetId {
            it[name] = "Starfall Scrollworks"
            it[description] = "Scrolls, sigils, and warded manuscripts."
            it[location] = "Ivory Quay"
        }

        val aetherBladeDescription = "Balanced steel for frontier wardens."
        val aetherBladeId = Products.insertAndGetId {
            it[name] = "Aether Blade"
            it[description] = aetherBladeDescription
            it[category] = ProductCategory.WEAPONS.name
            it[rarity] = Rarity.UNCOMMON.name
            it[price] = 320
            it[currency] = currencyId
            it[merchant] = blackforgeId
            it[stock] = 12
            it[createdAt] = Instant.fromEpochMilliseconds(2_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
        }
        Weapons.insert {
            it[id] = aetherBladeId
            it[damage] = 14
            it[damageType] = DamageType.PHYSICAL.name
            it[weaponSlot] = WeaponSlot.MAIN_HAND.name
        }

        val moonwellDraughtDescription = "A cooling potion for exhausted scouts."
        val moonwellDraughtId = Products.insertAndGetId {
            it[name] = "Moonwell Draught"
            it[description] = moonwellDraughtDescription
            it[category] = ProductCategory.POTIONS.name
            it[rarity] = Rarity.COMMON.name
            it[price] = 90
            it[currency] = currencyId
            it[merchant] = moonwellId
            it[stock] = 6
            it[isActive] = false
            it[createdAt] = Instant.fromEpochMilliseconds(1_500)
            it[updatedAt] = Instant.fromEpochMilliseconds(1_500)
        }
        Potions.insert {
            it[id] = moonwellDraughtId
            it[effect] = "Restore stamina"
            it[duration] = 3
        }

        val runicScrollDescription = "Inscribed warding magic for expedition leaders."
        val runicScrollId = Products.insertAndGetId {
            it[name] = "Runic Scroll"
            it[description] = runicScrollDescription
            it[category] = ProductCategory.SCROLLS.name
            it[rarity] = Rarity.RARE.name
            it[price] = 150
            it[currency] = currencyId
            it[merchant] = starfallId
            it[stock] = 3
            it[createdAt] = Instant.fromEpochMilliseconds(1_800)
            it[updatedAt] = Instant.fromEpochMilliseconds(1_800)
        }
        Scrolls.insert {
            it[id] = runicScrollId
            it[spellName] = "Aegis Ward"
            it[spellLevel] = 2
        }

        val aldricId = Characters.insertAndGetId {
            it[name] = "Aldric"
        }
        val brannaId = Characters.insertAndGetId {
            it[name] = "Branna"
        }

        val ravenShippingId = ShippingMethods.insertAndGetId {
            it[name] = "Courier Raven"
            it[description] = "Standard raven-assisted fulfillment."
            it[baseCost] = 20
            it[currency] = currencyId
            it[estimatedDays] = 2
        }
        val alchemyShippingId = ShippingMethods.insertAndGetId {
            it[name] = "Moonwell Express"
            it[description] = "Climate-controlled draught delivery."
            it[baseCost] = 15
            it[currency] = currencyId
            it[estimatedDays] = 1
        }
        val archiveShippingId = ShippingMethods.insertAndGetId {
            it[name] = "Archivist Courier"
            it[description] = "Insured parchment transit."
            it[baseCost] = 12
            it[currency] = currencyId
            it[estimatedDays] = 3
        }

        val pendingOrderId = Orders.insertAndGetId {
            it[character] = aldricId
            it[status] = OrderStatus.PENDING.name
            it[totalPrice] = 445
            it[totalCurrency] = currencyId
            it[createdAt] = Instant.fromEpochMilliseconds(5_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(5_000)
        }
        val blackforgeSubOrderId = SubOrders.insertAndGetId {
            it[order] = pendingOrderId
            it[merchant] = blackforgeId
            it[status] = OrderStatus.CONFIRMED.name
            it[shippingMethod] = ravenShippingId
            it[shippingCost] = 20
            it[merchantTotalPrice] = 340
            it[createdAt] = Instant.fromEpochMilliseconds(5_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(5_200)
        }
        val moonwellSubOrderId = SubOrders.insertAndGetId {
            it[order] = pendingOrderId
            it[merchant] = moonwellId
            it[status] = OrderStatus.SHIPPED.name
            it[shippingMethod] = alchemyShippingId
            it[shippingCost] = 15
            it[merchantTotalPrice] = 105
            it[createdAt] = Instant.fromEpochMilliseconds(5_100)
            it[updatedAt] = Instant.fromEpochMilliseconds(5_400)
        }

        val aetherBladeOrderItemId = OrderItems.insertAndGetId {
            it[subOrder] = blackforgeSubOrderId
            it[product] = aetherBladeId
            it[quantity] = 1
            it[snapshottedPrice] = 320
            it[snapshottedCurrency] = currencyId
            it[createdAt] = Instant.fromEpochMilliseconds(5_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(5_000)
        }
        OrderItems.insertAndGetId {
            it[subOrder] = moonwellSubOrderId
            it[product] = moonwellDraughtId
            it[quantity] = 1
            it[snapshottedPrice] = 90
            it[snapshottedCurrency] = currencyId
            it[createdAt] = Instant.fromEpochMilliseconds(5_100)
            it[updatedAt] = Instant.fromEpochMilliseconds(5_100)
        }

        Reviews.insertAndGetId {
            it[character] = aldricId
            it[product] = aetherBladeId
            it[orderItem] = aetherBladeOrderItemId
            it[rating] = 4
            it[text] = "Reliable edge."
            it[createdAt] = Instant.fromEpochMilliseconds(5_500)
            it[updatedAt] = Instant.fromEpochMilliseconds(5_500)
        }

        Transactions.insertAndGetId {
            it[character] = aldricId
            it[currency] = currencyId
            it[amount] = -445
            it[type] = TransactionType.PURCHASE.name
            it[referenceId] = pendingOrderId.value
            it[referenceType] = "ORDER"
            it[description] = "Aldric paid 445 GOLD."
            it[createdAt] = Instant.fromEpochMilliseconds(5_600)
            it[updatedAt] = Instant.fromEpochMilliseconds(5_600)
        }

        val deliveredOrderId = Orders.insertAndGetId {
            it[character] = brannaId
            it[status] = OrderStatus.DELIVERED.name
            it[totalPrice] = 162
            it[totalCurrency] = currencyId
            it[createdAt] = Instant.fromEpochMilliseconds(1_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
        }
        val starfallSubOrderId = SubOrders.insertAndGetId {
            it[order] = deliveredOrderId
            it[merchant] = starfallId
            it[status] = OrderStatus.DELIVERED.name
            it[shippingMethod] = archiveShippingId
            it[shippingCost] = 12
            it[merchantTotalPrice] = 162
            it[createdAt] = Instant.fromEpochMilliseconds(1_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(2_000)
        }
        OrderItems.insertAndGetId {
            it[subOrder] = starfallSubOrderId
            it[product] = runicScrollId
            it[quantity] = 1
            it[snapshottedPrice] = 150
            it[snapshottedCurrency] = currencyId
            it[createdAt] = Instant.fromEpochMilliseconds(1_000)
            it[updatedAt] = Instant.fromEpochMilliseconds(1_000)
        }

        AdminAppScenario(
            database = database,
            primaryProduct = AdminProductFixture(
                id = ProductId(aetherBladeId.value),
                name = "Aether Blade",
                merchantName = "Blackforge Armory",
                description = aetherBladeDescription,
                category = ProductCategory.WEAPONS,
                price = 320,
                currencyCode = currencyCode,
                stock = 12,
                isActive = true,
                reviewText = "4.0 / 5 from 1 reviews",
                categoryFieldLabel = "Damage",
                categoryFieldValue = "14"
            ),
            inactiveProduct = AdminProductFixture(
                id = ProductId(moonwellDraughtId.value),
                name = "Moonwell Draught",
                merchantName = "Moonwell Remedies",
                description = moonwellDraughtDescription,
                category = ProductCategory.POTIONS,
                price = 90,
                currencyCode = currencyCode,
                stock = 6,
                isActive = false,
                reviewText = "No reviews yet",
                categoryFieldLabel = "Effect",
                categoryFieldValue = "Restore stamina"
            ),
            extraProduct = AdminProductFixture(
                id = ProductId(runicScrollId.value),
                name = "Runic Scroll",
                merchantName = "Starfall Scrollworks",
                description = runicScrollDescription,
                category = ProductCategory.SCROLLS,
                price = 150,
                currencyCode = currencyCode,
                stock = 3,
                isActive = true,
                reviewText = "No reviews yet",
                categoryFieldLabel = "Spell",
                categoryFieldValue = "Aegis Ward"
            ),
            newestOrder = AdminOrderFixture(
                orderId = OrderId(pendingOrderId.value),
                characterName = "Aldric",
                initialStatus = OrderStatus.PENDING,
                merchantCount = 2,
                totalPrice = 445,
                currencyCode = currencyCode,
                itemNames = listOf("Aether Blade", "Moonwell Draught"),
                historyDescription = "Aldric paid 445 GOLD."
            ),
            olderOrder = AdminOrderFixture(
                orderId = OrderId(deliveredOrderId.value),
                characterName = "Branna",
                initialStatus = OrderStatus.DELIVERED,
                merchantCount = 1,
                totalPrice = 162,
                currencyCode = currencyCode,
                itemNames = listOf("Runic Scroll"),
                historyDescription = "Order ${deliveredOrderId.value} is now DELIVERED."
            ),
            mutableSubOrder = AdminSubOrderFixture(
                id = SubOrderId(moonwellSubOrderId.value),
                merchantName = "Moonwell Remedies",
                initialStatus = OrderStatus.SHIPPED
            )
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
