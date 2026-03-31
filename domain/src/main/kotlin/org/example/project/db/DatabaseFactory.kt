package org.example.project.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.db.tables.*
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction as exposedSuspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun adminSchemaTables(): List<Table> = listOf(
    Characters,
    Currencies,
    CurrencyConversions,
    Transactions,
    Merchants,
    Products,
    Weapons,
    Armors,
    Potions,
    Scrolls,
    ShippingMethods,
    MerchantShippingMethods,
    CartItems,
    WishlistItems,
    Orders,
    SubOrders,
    OrderItems,
    Reviews,
)

fun Database.createTables(): Database = apply {
    transaction(this) {
        val actualTables = SchemaUtils.listTables().map { it.lowercase() }.toSet()
        adminSchemaTables()
            .map { it.tableName }
            .filterNot { tableName -> tableName.lowercase() in actualTables }
        SchemaUtils.create(*adminSchemaTables().toTypedArray())
    }
}

suspend fun <A> Database.suspendTransaction(
    readOnly: Boolean? = null,
    block: suspend Transaction.() -> A
): A = withContext(Dispatchers.IO) {
    exposedSuspendTransaction(db = this@suspendTransaction, readOnly = readOnly) { block() }
}
