package com.f4cets.mobile

data class User(
    val walletId: String = "",
    val role: String = "buyer",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val storeIds: List<String> = emptyList(),
    val profile: Profile = Profile(),
    val affiliateClicks: List<String> = emptyList(),
    val purchases: List<String> = emptyList(),
    val rewards: Double = 0.0
) {
    data class Profile(
        val name: String = "User",
        val avatar: String = "/assets/images/default-avatar.png",
        val email: String = "",
        val nfts: List<Map<String, Any>> = emptyList(),
        val shippingAddress: String = ""
    )
}