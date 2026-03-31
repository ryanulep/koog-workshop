package org.example.project.db.repository

import org.example.project.db.tables.Merchants
import org.example.project.domain.id.MerchantId
import org.example.project.domain.model.Merchant
import org.example.project.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class MerchantRepository {

    context(_: Transaction)
    fun getMerchants(offset: Long, limit: Long): Page<Merchant> {
        val items = Merchants.selectAll()
            .limit(limit.toInt()).offset(offset)
            .map(::mapToMerchant)
        val total = Merchants.selectAll().count()
        return Page(items, total, offset, limit)
    }

    context(_: Transaction)
    fun getAllMerchants(chunkSize: Long = 50L): Flow<List<Merchant>> = flow {
        var offset = 0L
        while (true) {
            val page = getMerchants(offset, chunkSize)
            if (page.items.isEmpty()) break
            emit(page.items)
            offset += chunkSize
        }
    }

    context(_: Transaction)
    fun getAllMerchants(): List<Merchant> =
        Merchants.selectAll().map(::mapToMerchant)

    context(_: Transaction)
    fun getMerchantOrNull(id: MerchantId): Merchant? =
        Merchants.selectAll().where { Merchants.id eq id.value }
            .map(::mapToMerchant)
            .singleOrNull()

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
