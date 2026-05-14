package org.example.project.domain.shared

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class CharacterId(val value: Uuid)

@Serializable
@JvmInline
value class CurrencyId(val value: Uuid)

@Serializable
@JvmInline
value class CurrencyConversionId(val value: Uuid)

@Serializable
@JvmInline
value class MerchantId(val value: Uuid)

@Serializable
@JvmInline
value class OrderId(val value: Uuid)

@Serializable
@JvmInline
value class SubOrderId(val value: Uuid)

@Serializable
@JvmInline
value class OrderItemId(val value: Uuid)

@Serializable
@JvmInline
value class ProductId(val value: Uuid)

@Serializable
@JvmInline
value class ReviewId(val value: Uuid)

@Serializable
@JvmInline
value class ShippingMethodId(val value: Uuid)

@Serializable
@JvmInline
value class TransactionId(val value: Uuid)

@Serializable
@JvmInline
value class CartItemId(val value: Uuid)

@Serializable
@JvmInline
value class WishlistItemId(val value: Uuid)
