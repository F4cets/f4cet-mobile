package com.f4cets.mobile.model

sealed class Item {
    data class Store(val store: StoreItem) : Item()
    data class Product(val product: MarketplaceItem) : Item()
}