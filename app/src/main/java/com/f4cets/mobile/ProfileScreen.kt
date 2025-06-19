package com.f4cets.mobile

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import java.io.IOException
import java.util.*

@Composable
fun ProfileScreen(walletId: String, navController: NavController) {
    val db = Firebase.firestore
    val storage = Firebase.storage
    val coroutineScope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var affiliateClicks by remember { mutableStateOf<List<AffiliateClick>>(emptyList()) }
    var orderCards by remember { mutableStateOf<List<OrderCardData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var shippingAddress by remember { mutableStateOf("") }
    var nftMintAddress by remember { mutableStateOf("") }

    LaunchedEffect(walletId) {
        try {
            // Fetch user
            val userDoc = db.collection("users").document(walletId).get().await()
            user = userDoc.toObject<User>()
            user?.let {
                name = it.profile.name
                email = it.profile.email
                shippingAddress = it.profile.shippingAddress
            }

            // Fetch affiliate clicks
            val clickDocs = db.collection("users")
                .document(walletId)
                .collection("affiliateClicks")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
            affiliateClicks = clickDocs.documents.mapNotNull { doc ->
                val affiliateName = doc.getString("affiliateName") ?: return@mapNotNull null
                val timestamp = doc.getString("timestamp") ?: return@mapNotNull null
                AffiliateClick(affiliateName, timestamp)
            }

            // Fetch transactions (digital or rwi)
            val txDocs = db.collection("transactions")
                .whereEqualTo("buyerId", walletId)
                .whereIn("type", listOf("digital", "rwi"))
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
            orderCards = txDocs.documents.mapNotNull { doc ->
                val transId = doc.id
                val amount = doc.getDouble("amount")?.toString() ?: "0"
                val currency = doc.getString("currency") ?: "SOL"
                val status = doc.getString("status") ?: "Ordered"
                val createdAt = doc.getString("createdAt")?.let { formatDate(it) } ?: "Unknown"
                val productIds = doc.get("productIds") as? List<String> ?: emptyList()
                if (productIds.isNotEmpty()) {
                    val productId = productIds[0]
                    val productDoc = db.collection("products").document(productId).get().await()
                    val productName = productDoc.getString("name") ?: "Unknown Product"
                    val imageUrl = productDoc.getString("selectedImage") ?: ""
                    val type = productDoc.getString("type") ?: ""
                    if (type == "digital" || type == "rwi") {
                        OrderCardData(productName, createdAt, "$amount $currency", status, imageUrl, transId)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            isLoading = false
        } catch (e: Exception) {
            Log.e("ProfileScreen", "Failed to load data: ${e.message}", e)
            errorMessage = "Failed to load data: ${e.message}"
            isLoading = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            avatarUri = it
            coroutineScope.launch {
                try {
                    val storageRef = storage.reference.child("avatars/$walletId/${UUID.randomUUID()}.jpg")
                    storageRef.putFile(it).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    db.collection("users").document(walletId)
                        .update("profile.avatar", downloadUrl)
                        .await()
                    user = user?.copy(profile = user!!.profile.copy(avatar = downloadUrl))
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Failed to upload avatar: ${e.message}", e)
                    errorMessage = "Failed to upload avatar: ${e.message}"
                }
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    user?.let { userData ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = userData.profile.avatar,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { imagePicker.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = userData.profile.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = userData.profile.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when (userData.role) {
                                "seller" -> MaterialTheme.colorScheme.primary
                                "god" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        ) {
                            Text(
                                text = userData.role.replaceFirstChar { it.uppercase(Locale.getDefault()) },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Settings Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Profile Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = shippingAddress,
                            onValueChange = { shippingAddress = it },
                            label = { Text("Shipping Address") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        db.collection("users").document(walletId)
                                            .update(
                                                mapOf(
                                                    "profile.name" to name,
                                                    "profile.email" to email,
                                                    "profile.shippingAddress" to shippingAddress
                                                )
                                            )
                                            .await()
                                        user = userData.copy(
                                            profile = userData.profile.copy(
                                                name = name,
                                                email = email,
                                                shippingAddress = shippingAddress
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Log.e("ProfileScreen", "Failed to update profile: ${e.message}", e)
                                        errorMessage = "Failed to update profile: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Changes")
                        }
                        OutlinedTextField(
                            value = nftMintAddress,
                            onValueChange = { nftMintAddress = it },
                            label = { Text("NFT Mint Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val client = OkHttpClient()
                                        val requestBody = FormBody.Builder()
                                            .add("walletAddress", walletId)
                                            .add("mintAddress", nftMintAddress)
                                            .build()
                                        val request = Request.Builder()
                                            .url("https://nftverify-232592911911.us-central1.run.app")
                                            .post(requestBody)
                                            .build()
                                        client.newCall(request).enqueue(object : Callback {
                                            override fun onFailure(call: Call, e: IOException) {
                                                coroutineScope.launch {
                                                    Log.e("ProfileScreen", "NFT verification error: ${e.message}", e)
                                                    errorMessage = "NFT verification error: ${e.message}"
                                                }
                                            }

                                            override fun onResponse(call: Call, response: Response) {
                                                coroutineScope.launch {
                                                    if (response.isSuccessful) {
                                                        val responseBody = response.body?.string()
                                                        Log.i("ProfileScreen", "NFT verification successful: $responseBody")
                                                        errorMessage = "NFT verified successfully"
                                                        val userDoc = db.collection("users").document(walletId).get().await()
                                                        user = userDoc.toObject<User>()
                                                    } else {
                                                        val errorBody = response.body?.string()
                                                        Log.e("ProfileScreen", "NFT verification failed: ${response.code} - $errorBody")
                                                        errorMessage = "NFT verification failed: ${response.message}"
                                                    }
                                                }
                                            }
                                        })
                                    } catch (e: Exception) {
                                        Log.e("ProfileScreen", "NFT verification error: ${e.message}", e)
                                        errorMessage = "NFT verification error: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Verify NFT")
                        }
                    }
                }
            }

            // Badges Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "NFT Badges",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        userData.profile.nfts.forEach { nft ->
                            val version = nft["type"] as? String ?: "Unknown"
                            val verified = nft["verified"] as? Boolean ?: false
                            val sellerDiscount = (nft["sellerDiscount"] as? Double ?: 0.0) * 100
                            val buyerDiscount = (nft["buyerDiscount"] as? Double ?: 0.0) * 100
                            val leasable = nft["leasable"] as? Boolean ?: false
                            ListItem(
                                headlineContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (version == "V1") {
                                            Icon(
                                                painter = painterResource(id = R.drawable.v1badge),
                                                contentDescription = "V1 Badge",
                                                modifier = Modifier.size(24.dp),
                                                tint = Color.Unspecified
                                            )
                                        }
                                        Text(
                                            text = "$version NFT",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                supportingContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Seller Discount: ${sellerDiscount.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Buyer Discount: ${buyerDiscount.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Leasable: ${if (leasable) "Yes" else "No"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingContent = {
                                    Text(
                                        text = if (verified) "Verified" else "Not Verified",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                        if (userData.profile.nfts.isEmpty()) {
                            Text(
                                text = "No NFTs found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Affiliate Activity Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Affiliate Activity",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (affiliateClicks.isEmpty()) {
                            Text(
                                text = "No affiliate activity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            items(items = affiliateClicks.take(5)) { click ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Transparent
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = click.affiliateName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                text = click.timestamp.substring(0, 10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Marketplace Orders Section
            item {
                Text(
                    text = "Marketplace Orders",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
                if (orderCards.isEmpty()) {
                    Text(
                        text = "No orders found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
            // CHANGED: Removed OrderCard nesting, integrated data into Card
            items(items = orderCards.take(5)) { orderCard ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (orderCard.transId.isNotBlank()) {
                                Log.d("ProfileScreen", "Navigating to orderDetails/${orderCard.transId}")
                                navController.navigate("orderDetails/${orderCard.transId}")
                            } else {
                                Log.e("ProfileScreen", "Invalid transId for order: $orderCard")
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = orderCard.imageUrl,
                            contentDescription = "Order Image",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = orderCard.productName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = orderCard.orderDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = orderCard.amount,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = orderCard.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Error Message
            errorMessage?.let {
                item {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    } ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = errorMessage ?: "User not found",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}