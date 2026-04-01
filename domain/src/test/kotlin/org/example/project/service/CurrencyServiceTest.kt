package org.example.project.domain.currency

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

class CurrencyServiceTest {
    private lateinit var database: Database
    private lateinit var currencyService: CurrencyService

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_currency_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        currencyService = CurrencyService(database)
    }

    @Test
    fun testCreateAndGetCurrency() = runBlocking {
        val id = currencyService.createCurrency("GOLD", "Gold", "G")
        val currency = currencyService.getCurrencyOrNull(id)
        assertNotNull(currency)
        assertEquals("GOLD", currency.code)
        assertEquals("Gold", currency.name)
        assertEquals("G", currency.symbol)
    }

    @Test
    fun testGetAllCurrencies() = runBlocking {
        currencyService.createCurrency("GOLD", "Gold", "G")
        currencyService.createCurrency("SILVER", "Silver", "S")
        val currencies = currencyService.getAllCurrencies()
        assertEquals(2, currencies.size)
    }

    @Test
    fun testUpdateCurrency() = runBlocking {
        val id = currencyService.createCurrency("GOLD", "Gold", "G")
        val updated = currencyService.updateCurrency(id, name = "Pure Gold")
        assertTrue(updated)
        val currency = currencyService.getCurrencyOrNull(id)
        assertNotNull(currency)
        assertEquals("Pure Gold", currency.name)
    }

    @Test
    fun testDeleteCurrency() = runBlocking {
        val id = currencyService.createCurrency("GOLD", "Gold", "G")
        val deleted = currencyService.deleteCurrency(id)
        assertTrue(deleted)
        val currency = currencyService.getCurrencyOrNull(id)
        assertNull(currency)
    }

    @Test
    fun testSetAndGetConversionRate() = runBlocking {
        val goldId = currencyService.createCurrency("GOLD", "Gold", "G")
        val silverId = currencyService.createCurrency("SILVER", "Silver", "S")
        currencyService.setConversionRate(goldId, silverId, 10.0)
        val rate = currencyService.getConversionRateOrNull(goldId, silverId)
        assertNotNull(rate)
        assertEquals(10.0, rate)
    }

    @Test
    fun testSetConversionRateUpdatesExisting() = runBlocking {
        val goldId = currencyService.createCurrency("GOLD", "Gold", "G")
        val silverId = currencyService.createCurrency("SILVER", "Silver", "S")
        currencyService.setConversionRate(goldId, silverId, 10.0)
        currencyService.setConversionRate(goldId, silverId, 15.0)
        val rate = currencyService.getConversionRateOrNull(goldId, silverId)
        assertNotNull(rate)
        assertEquals(15.0, rate)
    }
}
