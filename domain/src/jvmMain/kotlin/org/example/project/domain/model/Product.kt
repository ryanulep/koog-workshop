package org.example.project.domain.model

import org.example.project.domain.enums.*

sealed class Product {
    abstract val id: Long
    abstract val name: String
    abstract val description: String?
    abstract val category: ProductCategory
    abstract val rarity: Rarity
    abstract val price: Long
    abstract val currencyId: Long
    abstract val merchantId: Long
    abstract val stock: Int
    abstract val imageUrl: String?
    abstract val isActive: Boolean
    abstract val createdAt: Long

    data class Weapon(
        override val id: Long,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: Long,
        override val merchantId: Long,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Long,
        val damage: Int,
        val damageType: DamageType,
        val weaponSlot: WeaponSlot
    ) : Product() {
        override val category = ProductCategory.WEAPONS
    }

    data class Armor(
        override val id: Long,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: Long,
        override val merchantId: Long,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Long,
        val defense: Int,
        val armorSlot: ArmorSlot
    ) : Product() {
        override val category = ProductCategory.ARMOR
    }

    data class Potion(
        override val id: Long,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: Long,
        override val merchantId: Long,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Long,
        val effect: String,
        val duration: Int?
    ) : Product() {
        override val category = ProductCategory.POTIONS
    }

    data class Scroll(
        override val id: Long,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: Long,
        override val merchantId: Long,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Long,
        val spellName: String,
        val spellLevel: Int
    ) : Product() {
        override val category = ProductCategory.SCROLLS
    }

    data class MiscItem(
        override val id: Long,
        override val name: String,
        override val description: String?,
        override val rarity: Rarity,
        override val price: Long,
        override val currencyId: Long,
        override val merchantId: Long,
        override val stock: Int,
        override val imageUrl: String?,
        override val isActive: Boolean,
        override val createdAt: Long
    ) : Product() {
        override val category = ProductCategory.MISCELLANEOUS
    }
}
