package org.example.project.domain.model

data class ShippingMethod(
    val id: Long,
    val name: String,
    val description: String?,
    val baseCost: Long,
    val currencyId: Long,
    val estimatedDays: Int,
    val isActive: Boolean
)
