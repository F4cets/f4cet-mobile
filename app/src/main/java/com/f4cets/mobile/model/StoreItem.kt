package com.f4cets.mobile.model

data class StoreItem(
    val storeId: String,
    val name: String,
    val description: String,
    val thumbnailUrl: String,
    val sellerId: String,
    val categories: List<String> // CHANGED: Added categories for filtering
)