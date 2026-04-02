package org.example.project.domain.character

import kotlinx.coroutines.runBlocking
import org.example.project.db.connectSqlite
import org.example.project.db.createTables
import org.example.project.domain.currency.CurrencyService
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.*

class CharacterServiceTest {
    private lateinit var database: Database
    private lateinit var characterService: CharacterService
    private lateinit var currencyService: CurrencyService

    @BeforeTest
    fun setup() {
        val testDbFile = java.io.File.createTempFile("test_character_", ".db").apply { deleteOnExit() }
        database = connectSqlite(testDbFile).createTables()
        characterService = CharacterService(database)
        currencyService = CurrencyService(database)
    }

    @Test
    fun testCreateAndGetCharacter() = runBlocking {
        val id = characterService.createCharacter("Gandalf")
        val character = characterService.getCharacterOrNull(id)
        assertNotNull(character)
        assertEquals("Gandalf", character.name)
    }

    @Test
    fun testUpdateCharacter() = runBlocking {
        val id = characterService.createCharacter("Gandalf")
        val updated = characterService.updateCharacter(id, "Gandalf the White")
        assertTrue(updated)
        val character = characterService.getCharacterOrNull(id)
        assertNotNull(character)
        assertEquals("Gandalf the White", character.name)
    }

    @Test
    fun testDeleteCharacter() = runBlocking {
        val id = characterService.createCharacter("Gandalf")
        val deleted = characterService.deleteCharacter(id)
        assertTrue(deleted)
        val character = characterService.getCharacterOrNull(id)
        assertNull(character)
    }

    @Test
    fun testDepositAndBalance() = runBlocking {
        val characterId = characterService.createCharacter("Gandalf")
        val goldId = currencyService.createCurrency("GOLD", "Gold", "G")
        characterService.deposit(characterId, goldId, 1000)
        val balance = characterService.getWalletBalance(characterId)
        assertEquals(1000L, balance[goldId])
    }

    @Test
    fun testTransactionHistory() = runBlocking {
        val characterId = characterService.createCharacter("Gandalf")
        val goldId = currencyService.createCurrency("GOLD", "Gold", "G")
        characterService.deposit(characterId, goldId, 500, "First deposit")
        characterService.deposit(characterId, goldId, 300, "Second deposit")
        val page = characterService.getTransactionHistory(characterId)
        assertEquals(2, page.items.size)
        assertEquals(2L, page.total)
        val amounts = page.items.map { it.amount }.toSet()
        assertTrue(amounts.contains(500L))
        assertTrue(amounts.contains(300L))
    }
}
