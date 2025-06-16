package com.f4cets.mobile.model

data class MarketplaceItem(
    val productId: String = "",
    val name: String = "",
    val description: String = "",
    val priceUsdc: Double = 0.0,
    val imageUrl: String = "",
    val cryptoBackOffer: String = "",
    val storeId: String = "",
    val sellerId: String = "",
    val type: String = "",
    val categories: List<String> = emptyList(),
    val variants: Map<String, Any>? = null // CHANGED: Added variants field
)