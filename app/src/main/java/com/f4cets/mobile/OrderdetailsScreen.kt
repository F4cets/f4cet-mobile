package com.f4cets.mobile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProductInfo(
    val productId: String,
    val name: String,
    val imageUrl: String,
    val type: String
)

@Composable
fun OrderdetailsScreen(orderId: String, navController: NavController) {
    val db = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()
    var transaction by remember { mutableStateOf<Map<String, Any>?>(null) }
    var products by remember { mutableStateOf<List<ProductInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Log orderId for debugging
    LaunchedEffect(orderId) {
        Log.d("OrderdetailsScreen", "Received orderId: $orderId")
        if (orderId.isBlank()) {
            errorMessage = "Invalid order ID"
            isLoading = false
            Log.e("OrderdetailsScreen", "Order ID is blank")
            return@LaunchedEffect
        }
        try {
            // Fetch transaction
            val txDoc = db.collection("transactions").document(orderId).get().await()
            if (txDoc.exists()) {
                transaction = txDoc.data
                Log.d("OrderdetailsScreen", "Transaction loaded: ${txDoc.data}")
                // Fetch products
                val productIds = txDoc.get("productIds") as? List<String> ?: emptyList()
                products = productIds.mapNotNull { productId ->
                    val productDoc = db.collection("products").document(productId).get().await()
                    if (productDoc.exists()) {
                        ProductInfo(
                            productId = productId,
                            name = productDoc.getString("name") ?: "Unknown",
                            imageUrl = productDoc.getString("selectedImage") ?: "",
                            type = productDoc.getString("type") ?: ""
                        )
                    } else {
                        Log.w("OrderdetailsScreen", "Product not found: $productId")
                        null
                    }
                }
            } else {
                errorMessage = "Order not found"
                Log.e("OrderdetailsScreen", "Order not found for ID: $orderId")
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("OrderdetailsScreen", "Failed to load order: ${e.message}", e)
            errorMessage = "Failed to load order: ${e.message}"
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    transaction?.let { txData ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back Button
            item {
                Row {
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Back",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Order Summary
            item {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Order Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Order #${txData.get("txId") as? String ?: orderId}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "Date: ${
                                        (txData.get("createdAt") as? String)?.let {
                                            try {
                                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                                                    .parse(it)?.let { date ->
                                                        SimpleDateFormat("MM/dd/yy", Locale.US).format(date)
                                                    } ?: "Unknown"
                                            } catch (e: Exception) {
                                                "Unknown"
                                            }
                                        } ?: "Unknown"
                                    }",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Total: ${txData.get("amount") as? Double ?: 0.0} ${txData.get("currency") as? String ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "Status: ${txData.get("status") as? String ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (txData.get("status") == "Ordered") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            // Items Section
            item {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Items",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (products.isEmpty()) {
                            Text(
                                text = "No items found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            items(products) { product ->
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
                                text = product.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Type: ${product.type}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = "Product Image",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        },
                        modifier = Modifier.clickable { /* Placeholder: Navigate to ProductScreen */ }
                    )
                }
            }

            // Shipping Details (if RWI)
            if (txData.get("type") == "rwi") {
                item {
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
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Shipping Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "Address: ${txData.get("shippingAddress") as? String ?: "N/A"}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "Tracking: ${txData.get("trackingNumber") as? String ?: "N/A"}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Timeline Section
            item {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Order Timeline",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val timeline = txData.get("timeline") as? List<Map<String, String>> ?: emptyList()
                        if (timeline.isEmpty()) {
                            Text(
                                text = "No timeline events",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            timeline.forEach { event ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = event.get("title") ?: "Unknown",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${event.get("date") ?: "N/A"}: ${event.get("description") ?: ""}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // cNFT Mints Section
            item {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "cNFT Mints",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val nftMints = txData.get("nftMints") as? List<String> ?: emptyList()
                        if (nftMints.isEmpty()) {
                            Text(
                                text = "No cNFTs minted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            nftMints.forEach { mintAddress ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = "Mint: ${mintAddress.take(8)}...${mintAddress.takeLast(8)}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Transfers Section
            item {
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Transfers",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val transfers = txData.get("transfers") as? List<Map<String, Any>> ?: emptyList()
                        if (transfers.isEmpty()) {
                            Text(
                                text = "No transfers recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            transfers.forEach { transfer ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = "Mint: ${(transfer.get("mintAddress") as? String)?.take(8)}...${(transfer.get("mintAddress") as? String)?.takeLast(8)}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    supportingContent = {
                                        Column {
                                            Text(
                                                text = "Status: ${transfer.get("status") as? String ?: "Unknown"}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Timestamp: ${transfer.get("timestamp") as? String ?: "N/A"}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Confirm Delivery Button (if not confirmed)
            if (!(txData.get("buyerConfirmed") as? Boolean ?: false) && txData.get("status") == "Shipped") {
                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    // Update buyerConfirmed
                                    db.collection("transactions").document(orderId)
                                        .update(
                                            mapOf(
                                                "buyerConfirmed" to true,
                                                "deliveryConfirmedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                                                "updatedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
                                            )
                                        )
                                        .await()
                                    // Call releaseFunds Cloud Function
                                    val client = OkHttpClient()
                                    val requestBody = FormBody.Builder()
                                        .add("orderId", orderId)
                                        .build()
                                    val request = Request.Builder()
                                        .url("https://releasefunds-232592911911.us-central1.run.app")
                                        .post(requestBody)
                                        .build()
                                    client.newCall(request).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            coroutineScope.launch {
                                                Log.e("OrderdetailsScreen", "Failed to release funds: ${e.message}", e)
                                                errorMessage = "Failed to release funds: ${e.message}"
                                            }
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            coroutineScope.launch {
                                                if (response.isSuccessful) {
                                                    Log.i("OrderdetailsScreen", "Funds released successfully")
                                                    errorMessage = "Delivery confirmed and funds released"
                                                    // Refresh transaction
                                                    val txDoc = db.collection("transactions").document(orderId).get().await()
                                                    transaction = txDoc.data
                                                } else {
                                                    val errorBody = response.body?.string()
                                                    Log.e("OrderdetailsScreen", "Failed to release funds: ${response.code} - $errorBody")
                                                    errorMessage = "Failed to release funds: ${response.message}"
                                                }
                                            }
                                        }
                                    })
                                } catch (e: Exception) {
                                    Log.e("OrderdetailsScreen", "Failed to confirm delivery: ${e.message}", e)
                                    errorMessage = "Failed to confirm delivery: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirm Delivery")
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
                text = errorMessage ?: "Order not found",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}