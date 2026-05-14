package org.example.project.domain.catalog

import org.example.project.domain.shared.*
import org.example.project.domain.shared.Page
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.example.project.db.update
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Service

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Service
class ProductRepository {

    private val joinedTable = Products
        .join(Weapons, JoinType.LEFT, Products.id, Weapons.id)
        .join(Armors, JoinType.LEFT, Products.id, Armors.id)
        .join(Potions, JoinType.LEFT, Products.id, Potions.id)
        .join(Scrolls, JoinType.LEFT, Products.id, Scrolls.id)

    fun getProducts(offset: Long, limit: Long): Page<Product> {
        val items = joinedTable.selectAll()
            .orderBy(Products.name)
            .limit(limit.toInt()).offset(offset)
            .map(::mapToProduct)
        val total = Products.selectAll().count()
        return Page(items, total, offset, limit)
    }

    fun getAllProducts(): List<Product> =
        joinedTable.selectAll().map(::mapToProduct)

    fun getProductOrNull(id: ProductId): Product? =
        joinedTable.selectAll().where { Products.id eq id.value }
            .map(::mapToProduct)
            .singleOrNull()

    fun getProductsByCategory(category: ProductCategory): List<Product> =
        joinedTable.selectAll().where { Products.category eq category.name }
            .map(::mapToProduct)

    fun updateStock(productId: ProductId, quantityChange: Int): Boolean {
        val currentStock = Products.selectAll().where { Products.id eq productId.value }
            .map { it[Products.stock] }
            .singleOrNull() ?: return false

        val newStock = currentStock + quantityChange
        if (newStock < 0) return false

        return Products.update(productId.value) {
            it[stock] = newStock
        } > 0
    }

    fun setProductActive(productId: ProductId, isActive: Boolean): Boolean =
        Products.update(productId.value) {
            it[Products.isActive] = isActive
        } > 0

    fun createProduct(product: Product): ProductId {
        require(product.price >= 0) { "Product price must be non-negative" }
        require(product.stock >= 0) { "Product stock must be non-negative" }
        val productId = ProductId(
            Products.insertAndGetId {
                it[name] = product.name
                it[description] = product.description
                it[category] = product.category.name
                it[rarity] = product.rarity.name
                it[price] = product.price
                it[currency] = product.currencyId.value
                it[merchant] = product.merchantId.value
                it[stock] = product.stock
                it[imageUrl] = product.imageUrl
                it[isActive] = product.isActive
            }.value
        )
        when (product) {
            is Product.Weapon -> Weapons.insert {
                it[id] = productId.value
                it[damage] = product.damage
                it[damageType] = product.damageType.name
                it[weaponSlot] = product.weaponSlot.name
            }
            is Product.Armor -> Armors.insert {
                it[id] = productId.value
                it[defense] = product.defense
                it[armorSlot] = product.armorSlot.name
            }
            is Product.Potion -> Potions.insert {
                it[id] = productId.value
                it[effect] = product.effect
                it[duration] = product.duration
            }
            is Product.Scroll -> Scrolls.insert {
                it[id] = productId.value
                it[spellName] = product.spellName
                it[spellLevel] = product.spellLevel
            }
            is Product.MiscItem -> { /* No detail table for misc items */ }
        }
        return productId
    }

    fun updateProduct(product: Product): Boolean {
        require(product.price >= 0) { "Product price must be non-negative" }
        require(product.stock >= 0) { "Product stock must be non-negative" }
        val updated = Products.update(product.id.value) {
            it[name] = product.name
            it[description] = product.description
            it[category] = product.category.name
            it[rarity] = product.rarity.name
            it[price] = product.price
            it[currency] = product.currencyId.value
            it[merchant] = product.merchantId.value
            it[stock] = product.stock
            it[imageUrl] = product.imageUrl
            it[isActive] = product.isActive
        } > 0
        if (!updated) return false
        when (product) {
            is Product.Weapon -> Weapons.update({ Weapons.id eq product.id.value }) {
                it[damage] = product.damage
                it[damageType] = product.damageType.name
                it[weaponSlot] = product.weaponSlot.name
            }
            is Product.Armor -> Armors.update({ Armors.id eq product.id.value }) {
                it[defense] = product.defense
                it[armorSlot] = product.armorSlot.name
            }
            is Product.Potion -> Potions.update({ Potions.id eq product.id.value }) {
                it[effect] = product.effect
                it[duration] = product.duration
            }
            is Product.Scroll -> Scrolls.update({ Scrolls.id eq product.id.value }) {
                it[spellName] = product.spellName
                it[spellLevel] = product.spellLevel
            }
            is Product.MiscItem -> { /* No detail table for misc items */ }
        }
        return true
    }

    fun deleteProduct(id: ProductId): Boolean {
        // Delete detail rows first (they reference Products)
        Weapons.deleteWhere { Weapons.id eq id.value }
        Armors.deleteWhere { Armors.id eq id.value }
        Potions.deleteWhere { Potions.id eq id.value }
        Scrolls.deleteWhere { Scrolls.id eq id.value }
        return Products.deleteWhere { Products.id eq id.value } > 0
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
