package org.example.project.domain.model

data class Merchant(
    val id: Long,
    val name: String,
    val description: String?,
    val location: String?,
    val theme: String?,
    val iconPath: String?,
    val isActive: Boolean
)
