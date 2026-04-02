package org.example.project.domain.shipping

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.catalog.CatalogService
import org.example.project.domain.currency.CurrencyService
import org.example.project.domain.shared.*
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ShippingServiceTest {

    private lateinit var database: Database
    private lateinit var shippingService: ShippingService
    private lateinit var catalogService: CatalogService
    private lateinit var currencyService: CurrencyService
    private var goldId: CurrencyId = CurrencyId(kotlin.uuid.Uuid.generateV7())
    private var merchantId: MerchantId = MerchantId(kotlin.uuid.Uuid.generateV7())

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_shipping_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        shippingService = ShippingService(database)
        catalogService = CatalogService(database)
        currencyService = CurrencyService(database)
        runBlocking {
            goldId = currencyService.createCurrency("GOLD", "Gold", "G")
            merchantId = catalogService.createMerchant("Test Merchant")
        }
    }

    @Test
    fun testCreateAndGetShippingMethod() = runBlocking {
        val id = shippingService.createShippingMethod(
            name = "Dragon Express",
            description = "Fast delivery by dragon",
            baseCost = 500,
            currencyId = goldId,
            estimatedDays = 1
        )
        val method = shippingService.getShippingMethodOrNull(id)
        assertNotNull(method)
        assertEquals("Dragon Express", method.name)
        assertEquals("Fast delivery by dragon", method.description)
        assertEquals(500L, method.baseCost)
        assertEquals(goldId, method.currencyId)
        assertEquals(1, method.estimatedDays)
        assertTrue(method.isActive)
    }

    @Test
    fun testGetAllShippingMethods() = runBlocking {
        shippingService.createShippingMethod(
            name = "Dragon Express",
            baseCost = 500,
            currencyId = goldId,
            estimatedDays = 1
        )
        shippingService.createShippingMethod(
            name = "Caravan Standard",
            baseCost = 100,
            currencyId = goldId,
            estimatedDays = 7
        )
        val methods = shippingService.getAllShippingMethods()
        assertEquals(2, methods.size)
    }

    @Test
    fun testUpdateShippingMethod() = runBlocking {
        val id = shippingService.createShippingMethod(
            name = "Dragon Express",
            baseCost = 500,
            currencyId = goldId,
            estimatedDays = 1
        )
        shippingService.updateShippingMethod(id, name = "Phoenix Express", baseCost = 750)
        val updated = shippingService.getShippingMethodOrNull(id)
        assertNotNull(updated)
        assertEquals("Phoenix Express", updated.name)
        assertEquals(750L, updated.baseCost)
    }

    @Test
    fun testCreateShippingMethodRejectsNegativeBaseCost() = runBlocking {
        val exception = assertFailsWith<IllegalArgumentException> {
            shippingService.createShippingMethod(
                name = "Broken Courier",
                baseCost = -1,
                currencyId = goldId,
                estimatedDays = 1
            )
        }
        assertTrue(exception.message!!.contains("non-negative"))
    }

    @Test
    fun testUpdateShippingMethodRejectsNegativeBaseCost() = runBlocking {
        val id = shippingService.createShippingMethod(
            name = "Dragon Express",
            baseCost = 500,
            currencyId = goldId,
            estimatedDays = 1
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            shippingService.updateShippingMethod(id, baseCost = -1)
        }
        assertTrue(exception.message!!.contains("non-negative"))
    }

    @Test
    fun testDeleteShippingMethod() = runBlocking {
        val id = shippingService.createShippingMethod(
            name = "Dragon Express",
            baseCost = 500,
            currencyId = goldId,
            estimatedDays = 1
        )
        shippingService.deleteShippingMethod(id)
        val method = shippingService.getShippingMethodOrNull(id)
        assertNull(method)
    }

    @Test
    fun testAddAndRemoveShippingMethodToMerchant() = runBlocking {
        val shippingMethodId = shippingService.createShippingMethod(
            name = "Dragon Express",
            baseCost = 500,
            currencyId = goldId,
            estimatedDays = 1
        )
        shippingService.addShippingMethodToMerchant(merchantId, shippingMethodId)
        val methods = shippingService.getMerchantShippingMethods(merchantId)
        assertEquals(1, methods.size)

        shippingService.removeShippingMethodFromMerchant(merchantId, shippingMethodId)
        val afterRemove = shippingService.getMerchantShippingMethods(merchantId)
        assertEquals(0, afterRemove.size)
    }
}
