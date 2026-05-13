package org.example.project.db

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update as exposedUpdate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
abstract class StoreTable(name: String) : IdTable<Uuid>(name) {
    override val id: Column<EntityID<Uuid>> = uuid("id")
        .clientDefault { Uuid.generateV7() }
        .entityId()
    override val primaryKey = PrimaryKey(id)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

fun <T : StoreTable> T.update(
    id: Uuid,
    limit: Int? = null,
    body: T.(UpdateStatement) -> Unit
): Int {
    return exposedUpdate({ this.id eq id }, limit) { statement ->
        body(statement)
        statement[updatedAt] = CurrentTimestamp
    }
}

@OptIn(ExperimentalUuidApi::class)
fun StoreTable.deleteById(id: Uuid): Boolean =
    deleteWhere { this.id eq id } > 0

@OptIn(ExperimentalUuidApi::class)
inline fun <R> StoreTable.findByIdOrNull(id: Uuid, mapper: (ResultRow) -> R): R? =
    selectAll().where { this.id eq id }.map(mapper).singleOrNull()