package org.example.project.domain.catalog

import org.example.project.domain.catalog.Merchants
import org.example.project.domain.shared.MerchantId
import org.example.project.domain.catalog.Merchant
import org.example.project.domain.shared.Page
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.example.project.db.update as storeUpdate

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class MerchantRepository {

    context(_: Transaction)
    fun getMerchants(offset: Long, limit: Long): Page<Merchant> {
        val items = Merchants.selectAll()
            .orderBy(Merchants.name)
            .limit(limit.toInt()).offset(offset)
            .map(::mapToMerchant)
        val total = Merchants.selectAll().count()
        return Page(items, total, offset, limit)
    }

    context(_: Transaction)
    fun getAllMerchants(): List<Merchant> =
        Merchants.selectAll().map(::mapToMerchant)

    context(_: Transaction)
    fun getMerchantOrNull(id: MerchantId): Merchant? =
        Merchants.selectAll().where { Merchants.id eq id.value }
            .map(::mapToMerchant)
            .singleOrNull()

    context(_: Transaction)
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

    context(_: Transaction)
    fun updateMerchant(
        id: MerchantId,
        name: String? = null,
        description: String? = null,
        location: String? = null,
        theme: String? = null,
        iconPath: String? = null,
        isActive: Boolean? = null
    ): Boolean =
        Merchants.storeUpdate({ Merchants.id eq id.value }) {
            if (name != null) it[Merchants.name] = name
            if (description != null) it[Merchants.description] = description
            if (location != null) it[Merchants.location] = location
            if (theme != null) it[Merchants.theme] = theme
            if (iconPath != null) it[Merchants.iconPath] = iconPath
            if (isActive != null) it[Merchants.isActive] = isActive
        } > 0

    context(_: Transaction)
    fun setMerchantActive(id: MerchantId, isActive: Boolean): Boolean =
        Merchants.storeUpdate({ Merchants.id eq id.value }) {
            it[Merchants.isActive] = isActive
        } > 0

    context(_: Transaction)
    fun deleteMerchant(id: MerchantId): Boolean =
        Merchants.deleteWhere { Merchants.id eq id.value } > 0

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
