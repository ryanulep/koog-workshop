package org.example.project.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.domain.cart.CartItems
import org.example.project.domain.catalog.Armors
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.catalog.Potions
import org.example.project.domain.catalog.Products
import org.example.project.domain.catalog.Scrolls
import org.example.project.domain.catalog.Weapons
import org.example.project.domain.character.Characters
import org.example.project.domain.character.Transactions
import org.example.project.domain.currency.Currencies
import org.example.project.domain.currency.CurrencyConversions
import org.example.project.domain.order.OrderItems
import org.example.project.domain.order.Orders
import org.example.project.domain.order.SubOrders
import org.example.project.domain.review.Reviews
import org.example.project.domain.shipping.MerchantShippingMethods
import org.example.project.domain.shipping.ShippingMethods
import org.example.project.domain.wishlist.WishlistItems
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.io.File
import java.nio.file.Path

fun connectSqlite(url: String): Database {
    val dataSource = SQLiteDataSource(SQLiteConfig().apply {
        enforceForeignKeys(true)
    }).apply {
        this.url = url
    }

    return Database.connect(dataSource)
}

fun connectSqlite(path: Path): Database =
    connectSqlite("jdbc:sqlite:${path.toAbsolutePath().normalize().toUri().toASCIIString()}")

fun connectSqlite(file: File): Database =
    connectSqlite(file.toPath())

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
        SchemaUtils.create(*adminSchemaTables().toTypedArray())
    }
}

suspend fun <A> Database.suspendTransaction(
    readOnly: Boolean? = null,
    block: suspend Transaction.() -> A
): A = withContext(Dispatchers.IO) {
    suspendTransaction(db = this@suspendTransaction, readOnly = readOnly) { block() }
}
