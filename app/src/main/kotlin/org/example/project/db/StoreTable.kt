package org.example.project.db

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import kotlin.time.Clock
import kotlin.time.Instant
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

context(_: Transaction)
fun <T : StoreTable> T.update(
    where: (() -> Op<Boolean>)? = null,
    limit: Int? = null,
    body: T.(UpdateStatement) -> Unit
): Int {
    val whereClause = where ?: { Op.TRUE }
    return exposedUpdate(whereClause, limit) { statement ->
        body(statement)
        statement[updatedAt] = CurrentTimestamp
    }
}
