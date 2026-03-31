package org.example.project.db

import org.example.project.db.tables.*
import org.example.project.domain.enums.TransactionType
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class CharacterAndCurrencyTest {

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:sqlite:file:test?mode=memory&cache=shared&foreign_keys=on", "org.sqlite.JDBC")
    }

    @Test
    fun testInsertCharacter() {
        transaction {
            SchemaUtils.create(Characters)
            val charId = Characters.insertAndGetId {
                it[name] = "Thorin"
            }
            val charName = Characters.selectAll().where { Characters.id eq charId }
                .single()[Characters.name]
            assertEquals("Thorin", charName)
        }
    }

    @Test
    fun testInsertCurrenciesUnique() {
        transaction {
            SchemaUtils.create(Currencies)
            Currencies.insert {
                it[code] = "GOLD"
                it[name] = "Gold Pieces"
                it[symbol] = "GP"
            }
            
            assertFails {
                Currencies.insert {
                    it[code] = "GOLD"
                    it[name] = "Another Gold"
                    it[symbol] = "G"
                }
            }
        }
    }

    @Test
    fun testCurrencyConversionUnique() {
        transaction {
            SchemaUtils.create(Currencies, CurrencyConversions)
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold Pieces"
                it[symbol] = "GP"
            }
            val crownId = Currencies.insertAndGetId {
                it[code] = "CROWN"
                it[name] = "Crowns"
                it[symbol] = "CR"
            }
            
            CurrencyConversions.insert {
                it[fromCurrency] = goldId
                it[toCurrency] = crownId
                it[rate] = 1.5
            }
            
            assertFails {
                CurrencyConversions.insert {
                    it[fromCurrency] = goldId
                    it[toCurrency] = crownId
                    it[rate] = 2.0
                }
            }
        }
    }

    @Test
    fun testLedgerBalance() {
        transaction {
            SchemaUtils.create(Characters, Currencies, Transactions)
            val charId = Characters.insertAndGetId { it[name] = "Thorin" }
            val goldId = Currencies.insertAndGetId {
                it[code] = "GOLD"
                it[name] = "Gold"
                it[symbol] = "G"
            }
            
            Transactions.insert {
                it[character] = charId
                it[currency] = goldId
                it[amount] = 1000
                it[type] = TransactionType.DEPOSIT.name
            }
            
            Transactions.insert {
                it[character] = charId
                it[currency] = goldId
                it[amount] = -250
                it[type] = TransactionType.PURCHASE.name
            }
            
            val totalBalance = Transactions
                .select(Transactions.amount.sum())
                .where { (Transactions.character eq charId) and (Transactions.currency eq goldId) }
                .single()[Transactions.amount.sum()] ?: 0L
                
            assertEquals(750L, totalBalance)
        }
    }

    @Test
    fun testWalletBalanceDerivation() {
        transaction {
            SchemaUtils.create(Characters, Currencies, Transactions)
            val charId = Characters.insertAndGetId { it[name] = "Thorin" }
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val crownId = Currencies.insertAndGetId { it[code] = "CROWN"; it[name] = "Crown"; it[symbol] = "C" }
            
            Transactions.insert { it[character] = charId; it[currency] = goldId; it[amount] = 500; it[type] = TransactionType.DEPOSIT.name }
            Transactions.insert { it[character] = charId; it[currency] = crownId; it[amount] = 100; it[type] = TransactionType.DEPOSIT.name }
            
            val balances = Transactions
                .select(Transactions.currency, Transactions.amount.sum())
                .where { Transactions.character eq charId }
                .groupBy(Transactions.currency)
                .associate { it[Transactions.currency].value to (it[Transactions.amount.sum()] ?: 0L) }
                
            assertEquals(500L, balances[goldId.value])
            assertEquals(100L, balances[crownId.value])
        }
    }

    @Test
    fun testExchangeAudit() {
        transaction {
            SchemaUtils.create(Characters, Currencies, Transactions)
            val charId = Characters.insertAndGetId { it[name] = "Thorin" }
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            val crownId = Currencies.insertAndGetId { it[code] = "CROWN"; it[name] = "Crown"; it[symbol] = "C" }
            
            val exchangeRefId = 12345L
            
            // Debit Gold
            Transactions.insert {
                it[character] = charId
                it[currency] = goldId
                it[amount] = -100
                it[type] = TransactionType.EXCHANGE_DEBIT.name
                it[referenceId] = exchangeRefId
                it[referenceType] = "EXCHANGE"
            }
            
            // Credit Crowns
            Transactions.insert {
                it[character] = charId
                it[currency] = crownId
                it[amount] = 150
                it[type] = TransactionType.EXCHANGE_CREDIT.name
                it[referenceId] = exchangeRefId
                it[referenceType] = "EXCHANGE"
            }
            
            val auditCount = Transactions.selectAll()
                .where { Transactions.referenceId eq exchangeRefId }
                .count()
                
            assertEquals(2L, auditCount)
        }
    }

    @Test
    fun testForeignKeyConstraint() {
        transaction {
            SchemaUtils.create(Currencies, Characters, Transactions)
            val goldId = Currencies.insertAndGetId { it[code] = "GOLD"; it[name] = "Gold"; it[symbol] = "G" }
            
            assertFails {
                Transactions.insert {
                    it[character] = EntityID(999L, Characters) // Nonexistent character
                    it[currency] = goldId
                    it[amount] = 100
                    it[type] = TransactionType.DEPOSIT.name
                }
            }
        }
    }
}
