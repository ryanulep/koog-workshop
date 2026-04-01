package org.example.project.domain.shared

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Immutable
@JvmInline
value class CharacterId(val value: Uuid)

@Immutable
@JvmInline
value class CurrencyId(val value: Uuid)

@Immutable
@JvmInline
value class CurrencyConversionId(val value: Uuid)

@Immutable
@JvmInline
value class MerchantId(val value: Uuid)

@Immutable
@JvmInline
value class OrderId(val value: Uuid)

@Immutable
@JvmInline
value class SubOrderId(val value: Uuid)

@Immutable
@JvmInline
value class OrderItemId(val value: Uuid)

@Immutable
@JvmInline
value class ProductId(val value: Uuid)

@Immutable
@JvmInline
value class ReviewId(val value: Uuid)

@Immutable
@JvmInline
value class ShippingMethodId(val value: Uuid)

@Immutable
@JvmInline
value class TransactionId(val value: Uuid)

@Immutable
@JvmInline
value class CartItemId(val value: Uuid)

@Immutable
@JvmInline
value class WishlistItemId(val value: Uuid)
