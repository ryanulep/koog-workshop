@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.example.project.admin

import androidx.navigation.NavBackStackEntry
import androidx.savedstate.read
import org.example.project.domain.shared.OrderId
import kotlin.uuid.Uuid

internal object AdminDetailRoute {
    const val orderHistory = "admin-order-history"
    const val orderIdArg = "orderId"
    const val orderDetail = "admin-detail/{$orderIdArg}"

    fun forOrder(orderId: OrderId): String =
        "admin-detail/${orderId.value}"
}

internal fun NavBackStackEntry?.selectedOrderIdOrNull(): OrderId? =
    this?.arguments
        ?.read { getString(AdminDetailRoute.orderIdArg) }
        ?.toOrderIdOrNull()

internal fun String.toOrderIdOrNull(): OrderId? =
    runCatching {
        OrderId(Uuid.parse(this))
    }.getOrNull()
