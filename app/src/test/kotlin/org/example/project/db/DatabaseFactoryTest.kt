package org.example.project.db

import org.example.project.domain.character.Characters
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class DatabaseFactoryTest {

    @Test
    fun testDatabaseInitialization() {
        val testDbFile = "smoke_test.db"
        // Ensure we start fresh
        java.io.File(testDbFile).delete()
        
        try {
            val database = connectSqlite(java.io.File(testDbFile))
            database.createTables()

            transaction(database) {
                // Verify that at least one table is created and usable
                Characters.insert {
                    it[name] = "SmokeTestCharacter"
                }

                val exists = Characters.selectAll().where { Characters.name eq "SmokeTestCharacter" }.any()
                assertTrue(exists, "Character should exist in the database")
            }
        } finally {
            java.io.File(testDbFile).delete()
        }
    }
}
