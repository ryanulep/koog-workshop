package org.example.project.db

import org.example.project.db.tables.*
import org.example.project.domain.enums.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class MerchantAndProductTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared&foreign_keys=on", "org.sqlite.JDBC")
    }

    @Test
    fun testSchemaCreation() {
        transaction {
            SchemaUtils.create(Characters, Currencies, CurrencyConversions, Transactions, Merchants, Products)
        }
    }

    @Test
    fun testInsertMerchant() {
        transaction {
            SchemaUtils.create(Merchants)
            val merchantId = Merchants.insertAndGetId {
                it[name] = "Grimtooth's Armory"
                it[description] = "Best weapons in the realm"
                it[location] = "Ironforge"
                it[theme] = "Dwarven"
            }

            val merchant = Merchants.selectAll().where { Merchants.id eq merchantId }.single()
            assertEquals("Grimtooth's Armory", merchant[Merchants.name])
            assertEquals("Ironforge", merchant[Merchants.location])
            assertTrue(merchant[Merchants.isActive])
        }
    }

    @Test
    fun testMerchantUniqueName() {
        transaction {
            SchemaUtils.create(Merchants)
            Merchants.insert { it[name] = "Unique Merchant" }
            assertFails {
                Merchants.insert { it[name] = "Unique Merchant" }
            }
        }
    }

    @Test
    fun testInsertWeapon() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Weapon Smith" }

            val productId = Products.insertAndGetId {
                it[name] = "Longsword"
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 150
                it[currency] = goldId
                it[merchant] = merchantId
                it[damage] = 8
                it[damageType] = DamageType.SLASHING.name
                it[weaponSlot] = WeaponSlot.MAIN_HAND.name
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Longsword", product[Products.name])
            assertEquals(8, product[Products.damage])
            assertNull(product[Products.defense])
            assertNull(product[Products.effect])
        }
    }

    @Test
    fun testInsertArmor() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Armor Smith" }

            val productId = Products.insertAndGetId {
                it[name] = "Plate Mail"
                it[category] = ProductCategory.ARMOR.name
                it[rarity] = Rarity.RARE.name
                it[price] = 500
                it[currency] = goldId
                it[merchant] = merchantId
                it[defense] = 18
                it[armorSlot] = ArmorSlot.CHEST.name
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals(18, product[Products.defense])
            assertNull(product[Products.damage])
        }
    }

    @Test
    fun testInsertPotion() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Alchemist" }

            val productId = Products.insertAndGetId {
                it[name] = "Health Potion"
                it[category] = ProductCategory.POTIONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 50
                it[currency] = goldId
                it[merchant] = merchantId
                it[effect] = "Heals 50 HP"
                it[duration] = 0
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Heals 50 HP", product[Products.effect])
        }
    }

    @Test
    fun testInsertScroll() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Wizard" }

            val productId = Products.insertAndGetId {
                it[name] = "Fireball Scroll"
                it[category] = ProductCategory.SCROLLS.name
                it[rarity] = Rarity.UNCOMMON.name
                it[price] = 200
                it[currency] = goldId
                it[merchant] = merchantId
                it[spellName] = "Fireball"
                it[spellLevel] = 3
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Fireball", product[Products.spellName])
            assertEquals(3, product[Products.spellLevel])
        }
    }

    @Test
    fun testInsertMiscItem() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "General Store" }

            val productId = Products.insertAndGetId {
                it[name] = "Rope"
                it[category] = ProductCategory.MISCELLANEOUS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 10
                it[currency] = goldId
                it[merchant] = merchantId
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Rope", product[Products.name])
            assertNull(product[Products.damage])
            assertNull(product[Products.defense])
            assertNull(product[Products.effect])
            assertNull(product[Products.spellName])
        }
    }

    @Test
    fun testProductFK() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Valid Merchant" }

            // Fails due to invalid merchant
            assertFails {
                Products.insert {
                    it[name] = "Invalid Product"
                    it[category] = ProductCategory.MISCELLANEOUS.name
                    it[rarity] = Rarity.COMMON.name
                    it[price] = 10
                    it[currency] = goldId
                    it[merchant] = EntityID(999L, Merchants)
                }
            }

            // Fails due to invalid currency
            assertFails {
                Products.insert {
                    it[name] = "Invalid Product"
                    it[category] = ProductCategory.MISCELLANEOUS.name
                    it[rarity] = Rarity.COMMON.name
                    it[price] = 10
                    it[currency] = EntityID(999L, Currencies)
                    it[merchant] = merchantId
                }
            }
        }
    }

    @Test
    fun testStockDecrement() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Merchant" }
            val productId = Products.insertAndGetId {
                it[name] = "Item"
                it[category] = ProductCategory.MISCELLANEOUS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 10
                it[currency] = goldId
                it[merchant] = merchantId
                it[stock] = 10
            }

            Products.update({ Products.id eq productId }) {
                it[stock] = 9
            }

            val updatedStock = Products.selectAll().where { Products.id eq productId }.single()[Products.stock]
            assertEquals(9, updatedStock)
        }
    }

    @Test
    fun testFilterByCategory() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Merchant" }
            
            Products.insert {
                it[name] = "Sword"; it[category] = ProductCategory.WEAPONS.name; it[rarity] = Rarity.COMMON.name
                it[price] = 10; it[currency] = goldId; it[merchant] = merchantId
            }
            Products.insert {
                it[name] = "Axe"; it[category] = ProductCategory.WEAPONS.name; it[rarity] = Rarity.COMMON.name
                it[price] = 10; it[currency] = goldId; it[merchant] = merchantId
            }
            Products.insert {
                it[name] = "Shield"; it[category] = ProductCategory.ARMOR.name; it[rarity] = Rarity.COMMON.name
                it[price] = 10; it[currency] = goldId; it[merchant] = merchantId
            }

            val weaponCount = Products.selectAll().where { Products.category eq ProductCategory.WEAPONS.name }.count()
            assertEquals(2L, weaponCount)
        }
    }

    @Test
    fun testFilterByRarity() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Merchant" }
            
            Products.insert {
                it[name] = "Common Item"; it[category] = ProductCategory.MISCELLANEOUS.name; it[rarity] = Rarity.COMMON.name
                it[price] = 10; it[currency] = goldId; it[merchant] = merchantId
            }
            Products.insert {
                it[name] = "Legendary Item"; it[category] = ProductCategory.MISCELLANEOUS.name; it[rarity] = Rarity.LEGENDARY.name
                it[price] = 1000; it[currency] = goldId; it[merchant] = merchantId
            }

            val legendaryCount = Products.selectAll().where { Products.rarity eq Rarity.LEGENDARY.name }.count()
            assertEquals(1L, legendaryCount)
        }
    }
}
