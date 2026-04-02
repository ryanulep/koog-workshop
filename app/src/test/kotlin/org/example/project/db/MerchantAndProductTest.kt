package org.example.project.db

import org.example.project.domain.catalog.*
import org.example.project.domain.character.*
import org.example.project.domain.currency.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.uuid.toKotlinUuid
import kotlin.test.*
import kotlin.uuid.Uuid

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class MerchantAndProductTest {

    @BeforeTest
    fun setup() {
        connectSqlite("jdbc:sqlite:file:test?mode=memory&cache=shared")
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
            SchemaUtils.create(Currencies, Merchants, Products, Weapons)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Weapon Smith" }

            val productId = Products.insertAndGetId {
                it[name] = "Longsword"
                it[category] = ProductCategory.WEAPONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 150
                it[currency] = goldId
                it[merchant] = merchantId
            }
            Weapons.insert {
                it[id] = productId
                it[damage] = 8
                it[damageType] = DamageType.SLASHING.name
                it[weaponSlot] = WeaponSlot.MAIN_HAND.name
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Longsword", product[Products.name])

            val weapon = Weapons.selectAll().where { Weapons.id eq productId }.single()
            assertEquals(8, weapon[Weapons.damage])
        }
    }

    @Test
    fun testInsertArmor() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products, Armors)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Armor Smith" }

            val productId = Products.insertAndGetId {
                it[name] = "Plate Mail"
                it[category] = ProductCategory.ARMOR.name
                it[rarity] = Rarity.RARE.name
                it[price] = 500
                it[currency] = goldId
                it[merchant] = merchantId
            }
            Armors.insert {
                it[id] = productId
                it[defense] = 18
                it[armorSlot] = ArmorSlot.CHEST.name
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Plate Mail", product[Products.name])

            val armor = Armors.selectAll().where { Armors.id eq productId }.single()
            assertEquals(18, armor[Armors.defense])
        }
    }

    @Test
    fun testInsertPotion() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products, Potions)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Alchemist" }

            val productId = Products.insertAndGetId {
                it[name] = "Health Potion"
                it[category] = ProductCategory.POTIONS.name
                it[rarity] = Rarity.COMMON.name
                it[price] = 50
                it[currency] = goldId
                it[merchant] = merchantId
            }
            Potions.insert {
                it[id] = productId
                it[effect] = "Heals 50 HP"
                it[duration] = 0
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Health Potion", product[Products.name])

            val potion = Potions.selectAll().where { Potions.id eq productId }.single()
            assertEquals("Heals 50 HP", potion[Potions.effect])
        }
    }

    @Test
    fun testInsertScroll() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products, Scrolls)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "Wizard" }

            val productId = Products.insertAndGetId {
                it[name] = "Fireball Scroll"
                it[category] = ProductCategory.SCROLLS.name
                it[rarity] = Rarity.UNCOMMON.name
                it[price] = 200
                it[currency] = goldId
                it[merchant] = merchantId
            }
            Scrolls.insert {
                it[id] = productId
                it[spellName] = "Fireball"
                it[spellLevel] = 3
            }

            val product = Products.selectAll().where { Products.id eq productId }.single()
            assertEquals("Fireball Scroll", product[Products.name])

            val scroll = Scrolls.selectAll().where { Scrolls.id eq productId }.single()
            assertEquals("Fireball", scroll[Scrolls.spellName])
            assertEquals(3, scroll[Scrolls.spellLevel])
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
        }
    }

    @Test
    fun testProductMonetaryAndStockConstraints() {
        transaction {
            SchemaUtils.create(Currencies, Merchants, Products)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val merchantId = Merchants.insertAndGetId { it[name] = "General Store" }

            assertFails {
                Products.insert {
                    it[name] = "Negative Price"
                    it[category] = ProductCategory.MISCELLANEOUS.name
                    it[rarity] = Rarity.COMMON.name
                    it[price] = -1
                    it[currency] = goldId
                    it[merchant] = merchantId
                    it[stock] = 1
                }
            }

            assertFails {
                Products.insert {
                    it[name] = "Negative Stock"
                    it[category] = ProductCategory.MISCELLANEOUS.name
                    it[rarity] = Rarity.COMMON.name
                    it[price] = 1
                    it[currency] = goldId
                    it[merchant] = merchantId
                    it[stock] = -1
                }
            }
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
                    it[merchant] = EntityID(Uuid.generateV7(), Merchants)
                }
            }

            // Fails due to invalid currency
            assertFails {
                Products.insert {
                    it[name] = "Invalid Product"
                    it[category] = ProductCategory.MISCELLANEOUS.name
                    it[rarity] = Rarity.COMMON.name
                    it[price] = 10
                    it[currency] = EntityID(Uuid.generateV7(), Currencies)
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
