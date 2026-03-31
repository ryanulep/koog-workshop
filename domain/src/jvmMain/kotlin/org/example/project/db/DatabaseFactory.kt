package org.example.project.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.db.tables.*
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction as exposedSuspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Database.createTables(): Database = apply {
    transaction(this) {
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
            Reviews,
        )
    }
}

suspend fun <A> Database.suspendTransaction(
    block: suspend Transaction.() -> A
): A = withContext(Dispatchers.IO) {
    exposedSuspendTransaction(db = this@suspendTransaction) { block() }
}
