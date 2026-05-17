package org.example.project.db

import org.example.project.domain.catalog.*
import org.example.project.domain.character.Characters
import org.example.project.domain.character.Transactions
import org.example.project.domain.currency.Currencies
import org.example.project.domain.order.OrderStatus
import org.example.project.domain.order.Orders
import org.example.project.domain.shared.*
import org.example.project.domain.shipping.ShippingMethods
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

fun Database.seedDemoDataIfEmpty(
    clock: Clock = Clock.System
): Database = apply {
    transaction(this) {
        if (!shouldSeedDemoData()) return@transaction
        AdminDemoDataSeeder(clock.now()).seed()
    }
}

private fun shouldSeedDemoData(): Boolean =
    listOf(
        Characters,
        Orders,
        Products,
        Merchants,
        Currencies,
        ShippingMethods
    )
        .all { table -> table.selectAll().count() == 0L }

private class AdminDemoDataSeeder(
    private val now: Instant
) {
    
    fun seed() {
        val gold = insertCurrency(
            code = "GOLD",
            name = "Gold Crowns",
            symbol = "GC"
        )

        val ravenCourier = insertShippingMethod(
            name = "Courier Raven",
            description = "Reliable post for compact gear and documents.",
            baseCost = 45L,
            currencyId = gold,
            estimatedDays = 3
        )
        val waystoneCaravan = insertShippingMethod(
            name = "Waystone Caravan",
            description = "Protected caravan for armor, kits, and bulk supplies.",
            baseCost = 80L,
            currencyId = gold,
            estimatedDays = 5
        )
        val gryphonExpress = insertShippingMethod(
            name = "Gryphon Express",
            description = "Premium fast delivery for urgent adventuring orders.",
            baseCost = 120L,
            currencyId = gold,
            estimatedDays = 2
        )

        val blackforge = insertMerchant(
            name = "Blackforge Armory",
            description = "Battle-tested steel and frontier gear.",
            location = "Ironridge Hold",
            theme = "smithy"
        )
        val moonroot = insertMerchant(
            name = "Moonroot Apothecary",
            description = "Field remedies for hunters, scouts, and healers.",
            location = "Silverfen",
            theme = "alchemist"
        )
        val starweave = insertMerchant(
            name = "Starweave Archives",
            description = "Arcane scrolls and expedition records.",
            location = "Asterfall",
            theme = "arcane"
        )
        val hearthglow = insertMerchant(
            name = "Hearthglow Outfitters",
            description = "Travel supplies for long roads and cold passes.",
            location = "Northgate",
            theme = "outfitter"
        )

        addShippingMethodToMerchant(blackforge, ravenCourier)
        addShippingMethodToMerchant(blackforge, waystoneCaravan)
        addShippingMethodToMerchant(moonroot, ravenCourier)
        addShippingMethodToMerchant(moonroot, gryphonExpress)
        addShippingMethodToMerchant(starweave, ravenCourier)
        addShippingMethodToMerchant(starweave, gryphonExpress)
        addShippingMethodToMerchant(hearthglow, waystoneCaravan)
        addShippingMethodToMerchant(hearthglow, gryphonExpress)

        val dawnforgedClaymore = insertWeapon(
            name = "Dawnforged Claymore",
            description = "A broadblade favored by caravan wardens.",
            rarity = Rarity.RARE,
            price = 1_450L,
            currencyId = gold,
            merchantId = blackforge,
            stock = 3,
            damage = 24,
            damageType = DamageType.PHYSICAL,
            weaponSlot = WeaponSlot.MAIN_HAND
        )
        val rangerHookblade = insertWeapon(
            name = "Ranger's Hookblade",
            description = "Balanced steel for close woodland skirmishes.",
            rarity = Rarity.UNCOMMON,
            price = 880L,
            currencyId = gold,
            merchantId = blackforge,
            stock = 7,
            damage = 15,
            damageType = DamageType.PHYSICAL,
            weaponSlot = WeaponSlot.MAIN_HAND
        )
        val bastionShield = insertArmor(
            name = "Bastion Kite Shield",
            description = "Layered oak and steel for mounted escorts.",
            rarity = Rarity.UNCOMMON,
            price = 980L,
            currencyId = gold,
            merchantId = blackforge,
            stock = 6,
            defense = 19,
            armorSlot = ArmorSlot.SHIELD
        )
        val wintercloakMantle = insertArmor(
            name = "Wintercloak Mantle",
            description = "A fur-lined mantle woven for mountain travel.",
            rarity = Rarity.RARE,
            price = 760L,
            currencyId = gold,
            merchantId = hearthglow,
            stock = 10,
            defense = 11,
            armorSlot = ArmorSlot.CHEST
        )
        val sunspiceDraught = insertPotion(
            name = "Sunspice Healing Draught",
            description = "Restorative tonic for field injuries.",
            rarity = Rarity.COMMON,
            price = 140L,
            currencyId = gold,
            merchantId = moonroot,
            stock = 5,
            effect = "Restores vitality after prolonged combat.",
            duration = 0
        )
        val frostfernAntidote = insertPotion(
            name = "Frostfern Antidote",
            description = "Neutralizes cold venom and marsh toxins.",
            rarity = Rarity.COMMON,
            price = 95L,
            currencyId = gold,
            merchantId = moonroot,
            stock = 8,
            effect = "Clears poison and slows frostbite onset.",
            duration = 0
        )
        val emberleafTonic = insertPotion(
            name = "Emberleaf Stamina Tonic",
            description = "Keeps watch captains alert during long marches.",
            rarity = Rarity.UNCOMMON,
            price = 110L,
            currencyId = gold,
            merchantId = moonroot,
            stock = 14,
            effect = "Boosts endurance for several hours.",
            duration = 6
        )
        val emberwakeScroll = insertScroll(
            name = "Scroll of Emberwake",
            description = "A controlled flame wave for siege lines.",
            rarity = Rarity.RARE,
            price = 620L,
            currencyId = gold,
            merchantId = starweave,
            stock = 4,
            spellName = "Emberwake",
            spellLevel = 3
        )
        val veiledStepsScroll = insertScroll(
            name = "Scroll of Veiled Steps",
            description = "Masks movement through moonlit terrain.",
            rarity = Rarity.UNCOMMON,
            price = 540L,
            currencyId = gold,
            merchantId = starweave,
            stock = 9,
            spellName = "Veiled Steps",
            spellLevel = 2
        )
        val astromancerNotes = insertMiscItem(
            name = "Astromancer Field Notes",
            description = "Annotated routes and omen charts for expeditions.",
            rarity = Rarity.UNCOMMON,
            price = 310L,
            currencyId = gold,
            merchantId = starweave,
            stock = 12
        )
        val quietPathsLantern = insertMiscItem(
            name = "Lantern of Quiet Paths",
            description = "Dampens glare while keeping camp trails visible.",
            rarity = Rarity.UNCOMMON,
            price = 260L,
            currencyId = gold,
            merchantId = hearthglow,
            stock = 5
        )
        val wayfarerBedroll = insertMiscItem(
            name = "Wayfarer's Bedroll",
            description = "Weatherproof bedroll with stitched rune lining.",
            rarity = Rarity.COMMON,
            price = 180L,
            currencyId = gold,
            merchantId = hearthglow,
            stock = 15
        )

        val scenarios = listOf(
            OrderScenario(
                characterName = "Aldric Stormvale",
                merchantId = blackforge,
                shippingMethod = waystoneCaravan,
                status = OrderStatus.DELIVERED,
                createdAt = now - 35.days,
                updatedAt = now - 30.days,
                lineItems = listOf(
                    LineItemSeed(dawnforgedClaymore, 1),
                    LineItemSeed(bastionShield, 1)
                )
            ),
            OrderScenario(
                characterName = "Seraphine Vale",
                merchantId = moonroot,
                shippingMethod = gryphonExpress,
                status = OrderStatus.DELIVERED,
                createdAt = now - 31.days,
                updatedAt = now - 29.days,
                lineItems = listOf(
                    LineItemSeed(sunspiceDraught, 3),
                    LineItemSeed(emberleafTonic, 2)
                )
            ),
            OrderScenario(
                characterName = "Brom Ironroot",
                merchantId = hearthglow,
                shippingMethod = waystoneCaravan,
                status = OrderStatus.CANCELLED,
                createdAt = now - 28.days,
                updatedAt = now - 27.days,
                lineItems = listOf(
                    LineItemSeed(wintercloakMantle, 1),
                    LineItemSeed(wayfarerBedroll, 1)
                ),
                addRefund = true
            ),
            OrderScenario(
                characterName = "Lyra Nightbloom",
                merchantId = starweave,
                shippingMethod = ravenCourier,
                status = OrderStatus.REFUNDED,
                createdAt = now - 24.days,
                updatedAt = now - 22.days,
                lineItems = listOf(
                    LineItemSeed(emberwakeScroll, 1),
                    LineItemSeed(astromancerNotes, 1)
                ),
                addRefund = true
            ),
            OrderScenario(
                characterName = "Toren Emberwake",
                merchantId = blackforge,
                shippingMethod = gryphonExpress,
                status = OrderStatus.DELIVERED,
                createdAt = now - 21.days,
                updatedAt = now - 18.days,
                lineItems = listOf(
                    LineItemSeed(rangerHookblade, 1),
                    LineItemSeed(bastionShield, 1)
                )
            ),
            OrderScenario(
                characterName = "Mira Quickstep",
                merchantId = moonroot,
                shippingMethod = ravenCourier,
                status = OrderStatus.PENDING,
                createdAt = now - 18.hours,
                updatedAt = now - 18.hours,
                lineItems = listOf(
                    LineItemSeed(frostfernAntidote, 2),
                    LineItemSeed(emberleafTonic, 1)
                )
            ),
            OrderScenario(
                characterName = "Kael Thornward",
                merchantId = hearthglow,
                shippingMethod = gryphonExpress,
                status = OrderStatus.CONFIRMED,
                createdAt = now - 36.hours,
                updatedAt = now - 30.hours,
                lineItems = listOf(
                    LineItemSeed(quietPathsLantern, 1),
                    LineItemSeed(wayfarerBedroll, 1)
                )
            ),
            // These active orders intentionally look unhealthy but are not yet formally reported.
            OrderScenario(
                characterName = "Isolde Frostfern",
                merchantId = blackforge,
                shippingMethod = waystoneCaravan,
                status = OrderStatus.SHIPPED,
                createdAt = now - 12.days,
                updatedAt = now - 10.days,
                lineItems = listOf(
                    LineItemSeed(wintercloakMantle, 1),
                    LineItemSeed(bastionShield, 1)
                )
            ),
            OrderScenario(
                characterName = "Dain Hollowmere",
                merchantId = starweave,
                shippingMethod = ravenCourier,
                status = OrderStatus.CRAFTING,
                createdAt = now - 9.days,
                updatedAt = now - 7.days,
                lineItems = listOf(
                    LineItemSeed(emberwakeScroll, 1),
                    LineItemSeed(veiledStepsScroll, 1)
                )
            ),
            OrderScenario(
                characterName = "Nyra Cinderwhisper",
                merchantId = moonroot,
                shippingMethod = gryphonExpress,
                status = OrderStatus.CONFIRMED,
                createdAt = now - 6.days,
                updatedAt = now - 5.days,
                lineItems = listOf(
                    LineItemSeed(sunspiceDraught, 4),
                    LineItemSeed(frostfernAntidote, 2)
                )
            )
        )

        scenarios.forEach { scenario ->
            seedOrderHistory(
                currencyId = gold,
                scenario = scenario
            )
        }
    }

    
    private fun seedOrderHistory(
        currencyId: CurrencyId,
        scenario: OrderScenario
    ) {
        val characterId = insertCharacter(scenario.characterName)
        val productTotal = scenario.lineItems.sumOf { item ->
            item.product.price * item.quantity
        }
        val orderTotal = productTotal + scenario.shippingMethod.baseCost

        addTransaction(
            characterId = characterId,
            currencyId = currencyId,
            amount = orderTotal + 2_500L,
            type = org.example.project.domain.character.TransactionType.DEPOSIT,
            description = "Initial wallet funding for ${scenario.characterName}",
            createdAt = scenario.createdAt - 12.hours
        )

        val orderId = insertOrder(
            characterId = characterId,
            status = scenario.status,
            totalPrice = orderTotal,
            currencyId = currencyId,
            createdAt = scenario.createdAt,
            updatedAt = scenario.updatedAt
        )

        val subOrderId = org.example.project.domain.order.SubOrders.insertAndGetId {
            it[order] = orderId.value
            it[merchant] = scenario.merchantId.value
            it[status] = scenario.status.name
            it[shippingMethod] = scenario.shippingMethod.id.value
            it[shippingCost] = scenario.shippingMethod.baseCost
            it[merchantTotalPrice] = orderTotal
            it[org.example.project.domain.order.SubOrders.createdAt] = scenario.createdAt
            it[org.example.project.domain.order.SubOrders.updatedAt] = scenario.updatedAt
        }.value

        scenario.lineItems.forEach { item ->
            org.example.project.domain.order.OrderItems.insert {
                it[subOrder] = subOrderId
                it[product] = item.product.id.value
                it[quantity] = item.quantity
                it[snapshottedPrice] = item.product.price
                it[snapshottedCurrency] = currencyId.value
                it[org.example.project.domain.order.OrderItems.createdAt] = scenario.createdAt
                it[org.example.project.domain.order.OrderItems.updatedAt] = scenario.updatedAt
            }
        }

        addTransaction(
            characterId = characterId,
            currencyId = currencyId,
            amount = -orderTotal,
            type = org.example.project.domain.character.TransactionType.PURCHASE,
            referenceId = orderId,
            description = "Purchase for order ${orderId.value}",
            createdAt = scenario.createdAt,
            updatedAt = scenario.createdAt
        )

        if (scenario.addRefund) {
            addTransaction(
                characterId = characterId,
                currencyId = currencyId,
                amount = orderTotal,
                type = org.example.project.domain.character.TransactionType.REFUND,
                referenceId = orderId,
                description = "Refund completed for order ${orderId.value}",
                createdAt = scenario.updatedAt,
                updatedAt = scenario.updatedAt
            )
        }
    }

    
    private fun insertCurrency(
        code: String,
        name: String,
        symbol: String
    ): CurrencyId = CurrencyId(
        Currencies.insertAndGetId {
            it[Currencies.code] = code
            it[Currencies.name] = name
            it[Currencies.symbol] = symbol
        }.value
    )

    
    private fun insertMerchant(
        name: String,
        description: String?,
        location: String?,
        theme: String?
    ): MerchantId = MerchantId(
        Merchants.insertAndGetId {
            it[Merchants.name] = name
            it[Merchants.description] = description
            it[Merchants.location] = location
            it[Merchants.theme] = theme
        }.value
    )

    
    private fun insertShippingMethod(
        name: String,
        description: String?,
        baseCost: Long,
        currencyId: CurrencyId,
        estimatedDays: Int
    ): ShippingMethodSeed = ShippingMethodSeed(
        id = ShippingMethodId(
                    ShippingMethods.insertAndGetId {
                        it[ShippingMethods.name] = name
                        it[ShippingMethods.description] = description
                        it[ShippingMethods.baseCost] = baseCost
                        it[currency] = currencyId.value
                        it[ShippingMethods.estimatedDays] = estimatedDays
                    }.value
                ),
        baseCost = baseCost
    )

    
    private fun addShippingMethodToMerchant(
        merchantId: MerchantId,
        shippingMethodSeed: ShippingMethodSeed
    ) {
        org.example.project.domain.shipping.MerchantShippingMethods.insert {
            it[merchant] = merchantId.value
            it[org.example.project.domain.shipping.MerchantShippingMethods.shippingMethod] = shippingMethodSeed.id.value
        }
    }

    
    private fun insertCharacter(name: String): CharacterId =
        CharacterId(
            Characters.insertAndGetId {
                it[Characters.name] = name
            }.value
        )

    
    private fun insertOrder(
        characterId: CharacterId,
        status: OrderStatus,
        totalPrice: Long,
        currencyId: CurrencyId,
        createdAt: Instant,
        updatedAt: Instant
    ): OrderId = OrderId(
        Orders.insertAndGetId {
            it[character] = characterId.value
            it[Orders.status] = status.name
            it[Orders.totalPrice] = totalPrice
            it[totalCurrency] = currencyId.value
            it[Orders.createdAt] = createdAt
            it[Orders.updatedAt] = updatedAt
        }.value
    )

    
    private fun addTransaction(
        characterId: CharacterId,
        currencyId: CurrencyId,
        amount: Long,
        type: org.example.project.domain.character.TransactionType,
        description: String,
        createdAt: Instant,
        updatedAt: Instant = createdAt,
        referenceId: OrderId? = null
    ) {
        Transactions.insert {
            it[character] = characterId.value
            it[currency] = currencyId.value
            it[Transactions.amount] = amount
            it[Transactions.type] = type.name
            it[Transactions.referenceId] = referenceId?.value
            it[Transactions.referenceType] = referenceId?.let { "ORDER" }
            it[Transactions.description] = description
            it[Transactions.createdAt] = createdAt
            it[Transactions.updatedAt] = updatedAt
        }
    }

    
    private fun insertWeapon(
        name: String,
        description: String?,
        rarity: Rarity,
        price: Long,
        currencyId: CurrencyId,
        merchantId: MerchantId,
        stock: Int,
        damage: Int,
        damageType: DamageType,
        weaponSlot: WeaponSlot
    ): SeedProduct {
        val productId = Products.insertAndGetId {
            it[Products.name] = name
            it[Products.description] = description
            it[category] = ProductCategory.WEAPONS.name
            it[Products.rarity] = rarity.name
            it[Products.price] = price
            it[currency] = currencyId.value
            it[merchant] = merchantId.value
            it[Products.stock] = stock
            it[isActive] = true
        }.value

        Weapons.insert {
            it[id] = productId
            it[Weapons.damage] = damage
            it[Weapons.damageType] = damageType.name
            it[Weapons.weaponSlot] = weaponSlot.name
        }

        return SeedProduct(ProductId(productId), price)
    }

    
    private fun insertArmor(
        name: String,
        description: String?,
        rarity: Rarity,
        price: Long,
        currencyId: CurrencyId,
        merchantId: MerchantId,
        stock: Int,
        defense: Int,
        armorSlot: ArmorSlot
    ): SeedProduct {
        val productId = Products.insertAndGetId {
            it[Products.name] = name
            it[Products.description] = description
            it[category] = ProductCategory.ARMOR.name
            it[Products.rarity] = rarity.name
            it[Products.price] = price
            it[currency] = currencyId.value
            it[merchant] = merchantId.value
            it[Products.stock] = stock
            it[isActive] = true
        }.value

        Armors.insert {
            it[id] = productId
            it[Armors.defense] = defense
            it[Armors.armorSlot] = armorSlot.name
        }

        return SeedProduct(ProductId(productId), price)
    }

    
    private fun insertPotion(
        name: String,
        description: String?,
        rarity: Rarity,
        price: Long,
        currencyId: CurrencyId,
        merchantId: MerchantId,
        stock: Int,
        effect: String,
        duration: Int
    ): SeedProduct {
        val productId = Products.insertAndGetId {
            it[Products.name] = name
            it[Products.description] = description
            it[category] = ProductCategory.POTIONS.name
            it[Products.rarity] = rarity.name
            it[Products.price] = price
            it[currency] = currencyId.value
            it[merchant] = merchantId.value
            it[Products.stock] = stock
            it[isActive] = true
        }.value

        Potions.insert {
            it[id] = productId
            it[Potions.effect] = effect
            it[Potions.duration] = duration
        }

        return SeedProduct(ProductId(productId), price)
    }

    
    private fun insertScroll(
        name: String,
        description: String?,
        rarity: Rarity,
        price: Long,
        currencyId: CurrencyId,
        merchantId: MerchantId,
        stock: Int,
        spellName: String,
        spellLevel: Int
    ): SeedProduct {
        val productId = Products.insertAndGetId {
            it[Products.name] = name
            it[Products.description] = description
            it[category] = ProductCategory.SCROLLS.name
            it[Products.rarity] = rarity.name
            it[Products.price] = price
            it[currency] = currencyId.value
            it[merchant] = merchantId.value
            it[Products.stock] = stock
            it[isActive] = true
        }.value

        Scrolls.insert {
            it[id] = productId
            it[Scrolls.spellName] = spellName
            it[Scrolls.spellLevel] = spellLevel
        }

        return SeedProduct(ProductId(productId), price)
    }

    
    private fun insertMiscItem(
        name: String,
        description: String?,
        rarity: Rarity,
        price: Long,
        currencyId: CurrencyId,
        merchantId: MerchantId,
        stock: Int
    ): SeedProduct {
        val productId = Products.insertAndGetId {
            it[Products.name] = name
            it[Products.description] = description
            it[category] = ProductCategory.MISCELLANEOUS.name
            it[Products.rarity] = rarity.name
            it[Products.price] = price
            it[currency] = currencyId.value
            it[merchant] = merchantId.value
            it[Products.stock] = stock
            it[isActive] = true
        }.value

        return SeedProduct(ProductId(productId), price)
    }
}

private data class SeedProduct(
    val id: ProductId,
    val price: Long
)

private data class ShippingMethodSeed(
    val id: ShippingMethodId,
    val baseCost: Long
)

private data class LineItemSeed(
    val product: SeedProduct,
    val quantity: Int
)

private data class OrderScenario(
    val characterName: String,
    val merchantId: MerchantId,
    val shippingMethod: ShippingMethodSeed,
    val status: OrderStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lineItems: List<LineItemSeed>,
    val addRefund: Boolean = false
)
