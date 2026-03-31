package org.example.project.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.example.project.db.tables.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelSuspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

object DatabaseFactory {
    fun init(path: String = "fantasy_store.db") {
        val driverClassName = "org.sqlite.JDBC"
        val jdbcUrl = if (path.contains("?")) "jdbc:sqlite:$path" else "jdbc:sqlite:$path?foreign_keys=on"
        Database.connect(jdbcUrl, driverClassName)

        transaction {
            SchemaUtils.create(
                Characters,
                Currencies,
                CurrencyConversions,
                Transactions,
                Merchants,
                Products,
                ShippingMethods,
                MerchantShippingMethods,
                CartItems,
                WishlistItems,
                Orders,
                SubOrders,
                OrderItems,
                Reviews
            )
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
