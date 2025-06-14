package com.f4cets.mobile.model

data class MarketplaceItem(
    val productId: String,
    val name: String,
    val description: String, // CHANGED: Added description
    val priceUsdc: Double,
    val imageUrl: String, // Maps to selectedImage
    val cryptoBackOffer: String,
    val storeId: String,
    val sellerId: String,
    val type: String,
    val categories: List<String> // CHANGED: Added categories for filtering
)