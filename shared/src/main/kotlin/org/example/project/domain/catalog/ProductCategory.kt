package org.example.project.domain.catalog

import kotlinx.serialization.Serializable

@Serializable
enum class ProductCategory {
    WEAPONS, ARMOR, POTIONS, SCROLLS, MISCELLANEOUS
}
