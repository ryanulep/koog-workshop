package org.example.project.db.repository

import org.example.project.db.suspendTransaction
import org.example.project.db.tables.Products
import org.example.project.domain.enums.*
import org.example.project.domain.model.Product
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ProductRepository(
    private val database: Database
) {

    suspend fun getAllProducts(): List<Product> = database.suspendTransaction {
        Products.selectAll().map(::mapToProduct)
    }

    suspend fun getProductById(id: Long): Product? = database.suspendTransaction {
        Products.selectAll().where { Products.id eq id }
            .map(::mapToProduct)
            .singleOrNull()
    }

    suspend fun getProductsByCategory(category: ProductCategory): List<Product> = database.suspendTransaction {
        Products.selectAll().where { Products.category eq category.name }
            .map(::mapToProduct)
    }

    suspend fun updateStock(productId: Long, quantityChange: Int): Boolean = database.suspendTransaction {
        val currentStock = Products.selectAll().where { Products.id eq productId }
            .map { it[Products.stock] }
            .singleOrNull() ?: return@suspendTransaction false
        
        val newStock = currentStock + quantityChange
        if (newStock < 0) return@suspendTransaction false
        
        Products.update({ Products.id eq productId }) {
            it[stock] = newStock
        } > 0
    }

    private fun mapToProduct(row: ResultRow): Product {
        val categoryName = row[Products.category]
        val category = ProductCategory.valueOf(categoryName)
        
        return when (category) {
            ProductCategory.WEAPONS -> Product.Weapon(
                id = row[Products.id].value,
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = row[Products.currency].value,
                merchantId = row[Products.merchant].value,
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                damage = row[Products.damage] ?: 0,
                damageType = DamageType.valueOf(row[Products.damageType] ?: DamageType.PHYSICAL.name),
                weaponSlot = WeaponSlot.valueOf(row[Products.weaponSlot] ?: WeaponSlot.MAIN_HAND.name)
            )
            ProductCategory.ARMOR -> Product.Armor(
                id = row[Products.id].value,
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = row[Products.currency].value,
                merchantId = row[Products.merchant].value,
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                defense = row[Products.defense] ?: 0,
                armorSlot = ArmorSlot.valueOf(row[Products.armorSlot] ?: ArmorSlot.CHEST.name)
            )
            ProductCategory.POTIONS -> Product.Potion(
                id = row[Products.id].value,
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = row[Products.currency].value,
                merchantId = row[Products.merchant].value,
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                effect = row[Products.effect] ?: "",
                duration = row[Products.duration]
            )
            ProductCategory.SCROLLS -> Product.Scroll(
                id = row[Products.id].value,
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = row[Products.currency].value,
                merchantId = row[Products.merchant].value,
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                spellName = row[Products.spellName] ?: "",
                spellLevel = row[Products.spellLevel] ?: 0
            )
            ProductCategory.MISCELLANEOUS -> Product.MiscItem(
                id = row[Products.id].value,
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = row[Products.currency].value,
                merchantId = row[Products.merchant].value,
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt]
            )
        }
    }
}
