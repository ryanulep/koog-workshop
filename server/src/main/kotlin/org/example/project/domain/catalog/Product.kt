package org.example.project.domain.catalog

import kotlin.time.Instant
import org.example.project.domain.shared.CurrencyId
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.shared.ProductId

sealed class Product {
    abstract val id: ProductId
    abstract val name: String
    abstract val description: String?
    abstract val category: ProductCategory
    abstract val rarity: Rarity
    abstract val price: Long
    abstract val currencyId: CurrencyId
    abstract val merchantId: MerchantId
    abstract val stock: Int
    abstract val imageUrl: String?
    abstract val isActive: Boolean
    abstract val createdAt: Instant
    abstract val updatedAt: Instant

    data class Weapon(
        override val id: ProductId,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: CurrencyId,
        override val merchantId: MerchantId,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val damage: Int,
        val damageType: DamageType,
        val weaponSlot: WeaponSlot
    ) : Product() {
        override val category = ProductCategory.WEAPONS
    }

    data class Armor(
        override val id: ProductId,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: CurrencyId,
        override val merchantId: MerchantId,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val defense: Int,
        val armorSlot: ArmorSlot
    ) : Product() {
        override val category = ProductCategory.ARMOR
    }

    data class Potion(
        override val id: ProductId,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: CurrencyId,
        override val merchantId: MerchantId,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val effect: String,
        val duration: Int?
    ) : Product() {
        override val category = ProductCategory.POTIONS
    }

    data class Scroll(
        override val id: ProductId,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: CurrencyId,
        override val merchantId: MerchantId,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val spellName: String,
        val spellLevel: Int
    ) : Product() {
        override val category = ProductCategory.SCROLLS
    }

    data class MiscItem(
        override val id: ProductId,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: CurrencyId,
        override val merchantId: MerchantId,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Instant,
        override val updatedAt: Instant
    ) : Product() {
        override val category = ProductCategory.MISCELLANEOUS
    }
}
