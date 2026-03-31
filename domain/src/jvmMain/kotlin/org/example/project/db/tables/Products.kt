package org.example.project.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Products : LongIdTable("products") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val category = varchar("category", 50)          // ProductCategory.name
    val rarity = varchar("rarity", 50)              // Rarity.name
    val price = long("price")
    val currency = reference("currency_id", Currencies)
    val merchant = reference("merchant_id", Merchants)
    val stock = integer("stock").default(0)
    val imageUrl = varchar("image_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }

    // Weapon-specific (nullable)
    val damage = integer("damage").nullable()
    val damageType = varchar("damage_type", 50).nullable()
    val weaponSlot = varchar("weapon_slot", 50).nullable()

    // Armor-specific (nullable)
    val defense = integer("defense").nullable()
    val armorSlot = varchar("armor_slot", 50).nullable()

    // Potion-specific (nullable)
    val effect = text("effect").nullable()
    val duration = integer("duration").nullable()

    // Scroll-specific (nullable)
    val spellName = varchar("spell_name", 255).nullable()
    val spellLevel = integer("spell_level").nullable()

    init {
        index(false, category)
        index(false, merchant)
        index(false, rarity)
    }
}
