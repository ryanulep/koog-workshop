package org.example.project.db.repository

import org.example.project.db.suspendTransaction
import org.example.project.db.tables.*
import org.example.project.domain.enums.*
import org.example.project.domain.id.*
import org.example.project.domain.model.Product
import org.example.project.domain.repository.ProductRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class ExposedProductRepository(
    private val database: Database
) : ProductRepository {

    private val joinedTable = Products
        .join(Weapons, JoinType.LEFT, Products.id, Weapons.id)
        .join(Armors, JoinType.LEFT, Products.id, Armors.id)
        .join(Potions, JoinType.LEFT, Products.id, Potions.id)
        .join(Scrolls, JoinType.LEFT, Products.id, Scrolls.id)

    override suspend fun getAllProducts(): List<Product> = database.suspendTransaction {
        joinedTable.selectAll().map(::mapToProduct)
    }

    override suspend fun getProductOrNull(id: ProductId): Product? = database.suspendTransaction {
        joinedTable.selectAll().where { Products.id eq id.value }
            .map(::mapToProduct)
            .singleOrNull()
    }

    override suspend fun getProductsByCategory(category: ProductCategory): List<Product> = database.suspendTransaction {
        joinedTable.selectAll().where { Products.category eq category.name }
            .map(::mapToProduct)
    }

    override suspend fun updateStock(productId: ProductId, quantityChange: Int): Boolean = database.suspendTransaction {
        val currentStock = Products.selectAll().where { Products.id eq productId.value }
            .map { it[Products.stock] }
            .singleOrNull() ?: return@suspendTransaction false
        
        val newStock = currentStock + quantityChange
        if (newStock < 0) return@suspendTransaction false
        
        Products.update({ Products.id eq productId.value }) {
            it[stock] = newStock
        } > 0
    }

    private fun mapToProduct(row: ResultRow): Product {
        val categoryName = row[Products.category]
        val category = ProductCategory.valueOf(categoryName)
        
        return when (category) {
            ProductCategory.WEAPONS -> Product.Weapon(
                id = ProductId(row[Products.id].value),
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = CurrencyId(row[Products.currency].value),
                merchantId = MerchantId(row[Products.merchant].value),
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                updatedAt = row[Products.updatedAt],
                damage = row[Weapons.damage],
                damageType = DamageType.valueOf(row[Weapons.damageType]),
                weaponSlot = WeaponSlot.valueOf(row[Weapons.weaponSlot]),
            )
            ProductCategory.ARMOR -> Product.Armor(
                id = ProductId(row[Products.id].value),
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = CurrencyId(row[Products.currency].value),
                merchantId = MerchantId(row[Products.merchant].value),
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                updatedAt = row[Products.updatedAt],
                defense = row[Armors.defense],
                armorSlot = ArmorSlot.valueOf(row[Armors.armorSlot]),
            )
            ProductCategory.POTIONS -> Product.Potion(
                id = ProductId(row[Products.id].value),
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = CurrencyId(row[Products.currency].value),
                merchantId = MerchantId(row[Products.merchant].value),
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                updatedAt = row[Products.updatedAt],
                effect = row[Potions.effect],
                duration = row[Potions.duration],
            )
            ProductCategory.SCROLLS -> Product.Scroll(
                id = ProductId(row[Products.id].value),
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = CurrencyId(row[Products.currency].value),
                merchantId = MerchantId(row[Products.merchant].value),
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                updatedAt = row[Products.updatedAt],
                spellName = row[Scrolls.spellName],
                spellLevel = row[Scrolls.spellLevel],
            )
            ProductCategory.MISCELLANEOUS -> Product.MiscItem(
                id = ProductId(row[Products.id].value),
                name = row[Products.name],
                description = row[Products.description],
                rarity = Rarity.valueOf(row[Products.rarity]),
                price = row[Products.price],
                currencyId = CurrencyId(row[Products.currency].value),
                merchantId = MerchantId(row[Products.merchant].value),
                stock = row[Products.stock],
                imageUrl = row[Products.imageUrl],
                isActive = row[Products.isActive],
                createdAt = row[Products.createdAt],
                updatedAt = row[Products.updatedAt],
            )
        }
    }
}
