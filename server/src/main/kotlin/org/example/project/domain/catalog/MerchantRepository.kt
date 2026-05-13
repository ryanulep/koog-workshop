package org.example.project.domain.catalog

import org.example.project.db.deleteById
import org.example.project.db.findByIdOrNull
import org.example.project.db.update
import org.example.project.domain.catalog.Merchants
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.catalog.Merchant
import org.example.project.domain.shared.Page
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Service

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Service
class MerchantRepository {

    
    fun getMerchants(offset: Long, limit: Long): Page<Merchant> {
        val items = Merchants.selectAll()
            .orderBy(Merchants.name)
            .limit(limit.toInt()).offset(offset)
            .map(::mapToMerchant)
        val total = Merchants.selectAll().count()
        return Page(items, total, offset, limit)
    }

    
    fun getAllMerchants(): List<Merchant> =
        Merchants.selectAll().map(::mapToMerchant)

    
    fun getMerchantOrNull(id: MerchantId): Merchant? =
        Merchants.findByIdOrNull(id.value, ::mapToMerchant)

    
    fun createMerchant(
        name: String,
        description: String? = null,
        location: String? = null,
        theme: String? = null,
        iconPath: String? = null
    ): MerchantId =
        MerchantId(
            Merchants.insertAndGetId {
                it[Merchants.name] = name
                it[Merchants.description] = description
                it[Merchants.location] = location
                it[Merchants.theme] = theme
                it[Merchants.iconPath] = iconPath
            }.value
        )

    
    fun updateMerchant(
        id: MerchantId,
        name: String? = null,
        description: String? = null,
        location: String? = null,
        theme: String? = null,
        iconPath: String? = null,
        isActive: Boolean? = null
    ): Boolean =
        Merchants.update(id.value) {
            if (name != null) it[Merchants.name] = name
            if (description != null) it[Merchants.description] = description
            if (location != null) it[Merchants.location] = location
            if (theme != null) it[Merchants.theme] = theme
            if (iconPath != null) it[Merchants.iconPath] = iconPath
            if (isActive != null) it[Merchants.isActive] = isActive
        } > 0

    
    fun setMerchantActive(id: MerchantId, isActive: Boolean): Boolean =
        Merchants.update(id.value) {
            it[Merchants.isActive] = isActive
        } > 0

    
    fun deleteMerchant(id: MerchantId): Boolean =
        Merchants.deleteById(id.value)

    private fun mapToMerchant(row: ResultRow) = Merchant(
        id = MerchantId(row[Merchants.id].value),
        name = row[Merchants.name],
        description = row[Merchants.description],
        location = row[Merchants.location],
        theme = row[Merchants.theme],
        iconPath = row[Merchants.iconPath],
        isActive = row[Merchants.isActive],
        createdAt = row[Merchants.createdAt],
        updatedAt = row[Merchants.updatedAt]
    )
}
