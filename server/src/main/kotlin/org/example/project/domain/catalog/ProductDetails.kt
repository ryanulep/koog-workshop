package org.example.project.domain.catalog

import org.jetbrains.exposed.v1.core.Table

object Weapons : Table("weapons") {
    val id = reference("id", Products)
    override val primaryKey = PrimaryKey(id)

    val damage = integer("damage")
    val damageType = varchar("damage_type", 50)
    val weaponSlot = varchar("weapon_slot", 50)
}

object Armors : Table("armors") {
    val id = reference("id", Products)
    override val primaryKey = PrimaryKey(id)

    val defense = integer("defense")
    val armorSlot = varchar("armor_slot", 50)
}

object Potions : Table("potions") {
    val id = reference("id", Products)
    override val primaryKey = PrimaryKey(id)

    val effect = text("effect")
    val duration = integer("duration").nullable()
}

object Scrolls : Table("scrolls") {
    val id = reference("id", Products)
    override val primaryKey = PrimaryKey(id)

    val spellName = varchar("spell_name", 255)
    val spellLevel = integer("spell_level")
}
