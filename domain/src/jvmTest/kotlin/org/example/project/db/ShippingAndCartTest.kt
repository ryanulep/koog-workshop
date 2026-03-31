package org.example.project.db

import org.example.project.db.tables.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class ShippingAndCartTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared&foreign_keys=on", "org.sqlite.JDBC")
    }

    @Test
    fun testSchemaCreation() {
        transaction {
            SchemaUtils.create(
                Characters, Currencies, CurrencyConversions, Transactions, 
                Merchants, Products, ShippingMethods, MerchantShippingMethods, 
                CartItems, WishlistItems
            )
        }
    }

    @Test
    fun testInsertShippingMethods() {
        transaction {
            SchemaUtils.create(Currencies, ShippingMethods)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            
            val ravenId = ShippingMethods.insertAndGetId {
                it[ShippingMethods.name] = "Courier Raven"
                it[ShippingMethods.description] = "Fast but limited weight"
                it[ShippingMethods.baseCost] = 50
                it[ShippingMethods.currency] = goldId
                it[ShippingMethods.estimatedDays] = 3
            }

            val raven = ShippingMethods.selectAll().where { ShippingMethods.id eq ravenId }.single()
            assertEquals("Courier Raven", raven[ShippingMethods.name])
            assertEquals(50, raven[ShippingMethods.baseCost])
        }
    }

    @Test
    fun testUniqueShippingName() {
        transaction {
            SchemaUtils.create(Currencies, ShippingMethods)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            
            ShippingMethods.insert {
                it[ShippingMethods.name] = "Unique Method"
                it[ShippingMethods.baseCost] = 100
                it[ShippingMethods.currency] = goldId
                it[ShippingMethods.estimatedDays] = 5
            }

            assertFails {
                ShippingMethods.insert {
                    it[ShippingMethods.name] = "Unique Method"
                    it[ShippingMethods.baseCost] = 200
                    it[ShippingMethods.currency] = goldId
                    it[ShippingMethods.estimatedDays] = 2
                }
            }
        }
    }

    @Test
    fun testMerchantShippingJoin() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, ShippingMethods, MerchantShippingMethods)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantA = Merchants.insertAndGetId { it[name] = "Merchant A" }
            val merchantB = Merchants.insertAndGetId { it[name] = "Merchant B" }
            
            val method1 = ShippingMethods.insertAndGetId {
                it[ShippingMethods.name] = "Method 1"
                it[ShippingMethods.baseCost] = 50
                it[ShippingMethods.currency] = goldId
                it[ShippingMethods.estimatedDays] = 1
            }
            val method2 = ShippingMethods.insertAndGetId {
                it[ShippingMethods.name] = "Method 2"
                it[ShippingMethods.baseCost] = 100
                it[ShippingMethods.currency] = goldId
                it[ShippingMethods.estimatedDays] = 2
            }

            MerchantShippingMethods.insert {
                it[MerchantShippingMethods.merchant] = merchantA
                it[MerchantShippingMethods.shippingMethod] = method1
            }
            MerchantShippingMethods.insert {
                it[MerchantShippingMethods.merchant] = merchantA
                it[MerchantShippingMethods.shippingMethod] = method2
            }
            MerchantShippingMethods.insert {
                it[MerchantShippingMethods.merchant] = merchantB
                it[MerchantShippingMethods.shippingMethod] = method1
            }

            val methodsA = MerchantShippingMethods.selectAll().where { MerchantShippingMethods.merchant eq merchantA }.count()
            val methodsB = MerchantShippingMethods.selectAll().where { MerchantShippingMethods.merchant eq merchantB }.count()
            
            assertEquals(2L, methodsA)
            assertEquals(1L, methodsB)
        }
    }

    @Test
    fun testCompositePK() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, ShippingMethods, MerchantShippingMethods)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Merchant" }
            val methodId = ShippingMethods.insertAndGetId {
                it[ShippingMethods.name] = "Method"
                it[ShippingMethods.baseCost] = 50
                it[ShippingMethods.currency] = goldId
                it[ShippingMethods.estimatedDays] = 1
            }

            MerchantShippingMethods.insert {
                it[MerchantShippingMethods.merchant] = merchantId
                it[MerchantShippingMethods.shippingMethod] = methodId
            }

            assertFails {
                MerchantShippingMethods.insert {
                    it[MerchantShippingMethods.merchant] = merchantId
                    it[MerchantShippingMethods.shippingMethod] = methodId
                }
            }
        }
    }

    @Test
    fun testCartOperations() {
        transaction {
            SchemaUtils.create(Characters, Currencies, Merchants, Products, CartItems)
            val charId = Characters.insertAndGetId { it[name] = "Hero" }
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Shop" }
            val prodId = Products.insertAndGetId {
                it[Products.name] = "Sword"
                it[Products.category] = "WEAPONS"
                it[Products.rarity] = "COMMON"
                it[Products.price] = 100
                it[Products.currency] = goldId
                it[Products.merchant] = merchantId
            }

            // Add item
            CartItems.insert {
                it[CartItems.character] = charId
                it[CartItems.product] = prodId
                it[CartItems.quantity] = 1
            }

            val item = CartItems.selectAll().where { CartItems.character eq charId }.single()
            assertEquals(prodId, item[CartItems.product])
            assertEquals(1, item[CartItems.quantity])

            // Unique character+product
            assertFails {
                CartItems.insert {
                    it[CartItems.character] = charId
                    it[CartItems.product] = prodId
                    it[CartItems.quantity] = 2
                }
            }

            // Update quantity
            CartItems.update({ (CartItems.character eq charId) and (CartItems.product eq prodId) }) {
                it[CartItems.quantity] = 3
            }
            assertEquals(3, CartItems.selectAll().where { CartItems.character eq charId }.single()[CartItems.quantity])

            // Remove item
            CartItems.deleteWhere { (CartItems.character eq charId) and (CartItems.product eq prodId) }
            assertEquals(0L, CartItems.selectAll().where { CartItems.character eq charId }.count())

            // Clear cart (all items for character)
            CartItems.insert {
                it[CartItems.character] = charId
                it[CartItems.product] = prodId
                it[CartItems.quantity] = 1
            }
            CartItems.deleteWhere { CartItems.character eq charId }
            assertEquals(0L, CartItems.selectAll().where { CartItems.character eq charId }.count())
        }
    }

    @Test
    fun testCartMultipleItems() {
        transaction {
            SchemaUtils.create(Characters, Currencies, Merchants, Products, CartItems)
            val charId = Characters.insertAndGetId { it[name] = "Hero" }
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantA = Merchants.insertAndGetId { it[name] = "Merchant A" }
            val merchantB = Merchants.insertAndGetId { it[name] = "Merchant B" }
            
            val prod1 = Products.insertAndGetId {
                it[Products.name] = "Sword"
                it[Products.category] = "WEAPONS"
                it[Products.rarity] = "COMMON"
                it[Products.price] = 100
                it[Products.currency] = goldId
                it[Products.merchant] = merchantA
            }
            val prod2 = Products.insertAndGetId {
                it[Products.name] = "Shield"
                it[Products.category] = "ARMOR"
                it[Products.rarity] = "COMMON"
                it[Products.price] = 150
                it[Products.currency] = goldId
                it[Products.merchant] = merchantA
            }
            val prod3 = Products.insertAndGetId {
                it[Products.name] = "Potion"
                it[Products.category] = "POTIONS"
                it[Products.rarity] = "COMMON"
                it[Products.price] = 50
                it[Products.currency] = goldId
                it[Products.merchant] = merchantB
            }

            CartItems.insert { it[CartItems.character] = charId; it[CartItems.product] = prod1; it[CartItems.quantity] = 1 }
            CartItems.insert { it[CartItems.character] = charId; it[CartItems.product] = prod2; it[CartItems.quantity] = 1 }
            CartItems.insert { it[CartItems.character] = charId; it[CartItems.product] = prod3; it[CartItems.quantity] = 1 }

            val itemsCount = CartItems.selectAll().where { CartItems.character eq charId }.count()
            assertEquals(3L, itemsCount)
        }
    }

    @Test
    fun testWishlistOperations() {
        transaction {
            SchemaUtils.create(Characters, Currencies, Merchants, Products, WishlistItems)
            val charId = Characters.insertAndGetId { it[name] = "Hero" }
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Shop" }
            val prodId = Products.insertAndGetId {
                it[Products.name] = "Sword"
                it[Products.category] = "WEAPONS"
                it[Products.rarity] = "COMMON"
                it[Products.price] = 100
                it[Products.currency] = goldId
                it[Products.merchant] = merchantId
            }

            // Add to wishlist
            WishlistItems.insert {
                it[WishlistItems.character] = charId
                it[WishlistItems.product] = prodId
            }

            assertEquals(1L, WishlistItems.selectAll().where { WishlistItems.character eq charId }.count())

            // Unique
            assertFails {
                WishlistItems.insert {
                    it[WishlistItems.character] = charId
                    it[WishlistItems.product] = prodId
                }
            }

            // Remove
            WishlistItems.deleteWhere { (WishlistItems.character eq charId) and (WishlistItems.product eq prodId) }
            assertEquals(0L, WishlistItems.selectAll().where { WishlistItems.character eq charId }.count())
        }
    }
}
