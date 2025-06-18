package com.f4cets.mobile

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.f4cets.mobile.model.MarketplaceItem
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.scale
import java.util.UUID
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun ProductScreen(
    userProfile: User.Profile?,
    productId: String,
    navigateBack: () -> Unit,
    navigateToAffiliates: () -> Unit,
    navigateToStore: (String) -> Unit,
    navigateToProduct: (String) -> Unit,
    navigateToCart: () -> Unit
) {
    var productItem by remember { mutableStateOf<MarketplaceItem?>(null) }
    var selectedSize by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var selectedQuantity by remember { mutableStateOf(1) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showCartNotification by remember { mutableStateOf(false) }
    var showInfoPopup by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"
    var sizes by remember { mutableStateOf<List<String>?>(null) }
    var colors by remember { mutableStateOf<List<String>?>(null) }
    var maxQuantity by remember { mutableStateOf(1) }
    var solPrice by remember { mutableStateOf(147.48f) }
    var priceUpdated by remember { mutableStateOf(false) }
    var imageUrls by remember { mutableStateOf<List<String>?>(null) }
    val pagerState = rememberPagerState(pageCount = { imageUrls?.size ?: 0 })

    // Fetch SOL price every 15 seconds
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            while (true) {
                try {
                    val request = Request.Builder()
                        .url("https://getsolprice-232592911911.us-central1.run.app")
                        .build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val json = response.body?.string() ?: ""
                        val jsonObject = JSONObject(json)
                        val newPrice = jsonObject.getJSONObject("solana").getDouble("usd").toFloat()
                        solPrice = newPrice
                        priceUpdated = true
                        Log.d("ProductScreen", "Fetched SOL price: $newPrice")
                        delay(500)
                        priceUpdated = false
                    } else {
                        Log.e("ProductScreen", "Failed to fetch SOL price: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("ProductScreen", "SOL price fetch error: ${e.message}")
                }
                delay(15000)
            }
        }
    }

    // Animation for SOL price
    val solPriceColor by animateColorAsState(
        targetValue = if (priceUpdated) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 500),
        label = "solPriceColor"
    )
    val solPriceScale by animateFloatAsState(
        targetValue = if (priceUpdated) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 500),
        label = "solPriceScale"
    )

    LaunchedEffect(productId) {
        if (productId.isEmpty()) {
            Log.e("ProductScreen", "Product ID is empty")
            return@LaunchedEffect
        }
        isLoading = true
        coroutineScope.launch {
            val db = FirebaseFirestore.getInstance()

            // Fetch product details
            Log.d("ProductScreen", "Fetching product: id=$productId")
            try {
                val productDoc = db.collection("products")
                    .document(productId)
                    .get()
                    .await()
                if (productDoc.exists()) {
                    val name = productDoc.getString("name") ?: ""
                    val description = productDoc.getString("description") ?: ""
                    val priceUsdc = (productDoc.get("price") as? Number)?.toDouble() ?: 0.0
                    val selectedImage = productDoc.getString("selectedImage") ?: ""
                    imageUrls = productDoc.get("imageUrls") as? List<String> ?: emptyList()
                    val imageUrl = selectedImage.ifEmpty { imageUrls?.firstOrNull() ?: "" }
                    val cryptoBackOffer = productDoc.getString("cryptoBackOffer") ?: "Upto 5% Crypto Cashback"
                    val storeId = productDoc.getString("storeId") ?: ""
                    val sellerId = productDoc.getString("sellerId") ?: ""
                    val type = productDoc.getString("type") ?: "digital"
                    val categories = productDoc.get("categories") as? List<String> ?: emptyList()
                    val variants = productDoc.get("variants") as? List<Map<String, Any>> ?: emptyList()
                    // Parse variants, quantity as string for RWI
                    val variantList = variants.mapNotNull { variantMap ->
                        val size = variantMap["size"] as? String
                        val color = variantMap["color"] as? String
                        val qty = when (val rawQty = variantMap["quantity"]) {
                            is String -> rawQty.toIntOrNull() ?: 0
                            is Number -> rawQty.toInt()
                            else -> 0
                        }
                        if (qty > 0) mapOf("size" to size, "color" to color, "quantity" to qty) else null
                    }
                    sizes = variantList.mapNotNull { it["size"] as? String }.distinct()
                    colors = variantList.mapNotNull { it["color"] as? String }.distinct()
                    maxQuantity = if (type == "rwi") {
                        variantList.filter { it["size"] == selectedSize && it["color"] == selectedColor }
                            .sumOf { it["quantity"] as Int }.coerceAtLeast(1)
                    } else {
                        (productDoc.get("quantity") as? Number)?.toInt()?.coerceAtLeast(1) ?: 1
                    }
                    productItem = MarketplaceItem(
                        productId = productId,
                        name = name,
                        description = description,
                        priceUsdc = priceUsdc,
                        imageUrl = imageUrl,
                        cryptoBackOffer = cryptoBackOffer,
                        storeId = storeId,
                        sellerId = sellerId,
                        type = type,
                        categories = categories,
                        variants = variants.associateBy { it["size"] as? String ?: "" }
                    )
                    Log.d("ProductScreen", "Product fetched: id=$productId, name=$name, imageUrls=$imageUrls")
                } else {
                    Log.e("ProductScreen", "Product not found: id=$productId")
                }
            } catch (e: Exception) {
                Log.e("ProductScreen", "Product fetch error: ${e.message}")
                productItem = null
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedSize, selectedColor, productItem) {
        if (productItem?.type == "rwi" && selectedSize != null && selectedColor != null) {
            val variant = productItem?.variants?.values?.mapNotNull { variantMap ->
                val map = variantMap as? Map<*, *>
                val s = map?.get("size") as? String
                val c = map?.get("color") as? String
                val qty = when (val rawQty = map?.get("quantity")) {
                    is String -> rawQty.toIntOrNull() ?: 0
                    is Number -> rawQty.toInt()
                    else -> 0
                }
                if (s == selectedSize && c == selectedColor) qty else null
            }?.firstOrNull() ?: 1
            maxQuantity = variant.coerceAtLeast(1)
            selectedQuantity = selectedQuantity.coerceIn(1, maxQuantity)
        } else if (productItem?.type == "digital") {
            coroutineScope.launch {
                val db = FirebaseFirestore.getInstance()
                val productDoc = db.collection("products").document(productId).get().await()
                maxQuantity = (productDoc.get("quantity") as? Number)?.toInt()?.coerceAtLeast(1) ?: 1
                selectedQuantity = selectedQuantity.coerceIn(1, maxQuantity)
            }
        }
    }

    val isAddToCartEnabled = productItem?.let { product ->
        if (product.type == "rwi") {
            selectedSize != null && selectedColor != null && selectedQuantity in 1..maxQuantity
        } else {
            selectedQuantity in 1..maxQuantity
        }
    } ?: false

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.bgp),
            contentDescription = "Product Background with Bag",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Avatar (top-left)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 24.dp, start = 24.dp)
                .size(56.dp)
                .align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val avatarUrl = userProfile?.avatar ?: ""
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.bgf)
                    )
                } else {
                    Text(
                        text = userProfile?.name?.take(1) ?: "A",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }

        // Shopping Cart FAB (top-right)
        FloatingActionButton(
            onClick = { navigateToCart() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(top = 24.dp, end = 24.dp)
                .size(56.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = "Shopping Cart"
            )
        }

        // Image carousel
        productItem?.let { product ->
            if (imageUrls != null && imageUrls!!.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp, start = 8.dp, end = 8.dp)
                        .height(300.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 0.1.dp
                ) { page ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        AsyncImage(
                            model = imageUrls!![page],
                            contentDescription = "Product Image for ${product.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            error = painterResource(id = R.drawable.bgf)
                        )
                    }
                }
            }
        }

        // Prices stacked
        productItem?.let { product ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 410.dp, start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$${String.format("%.2f", product.priceUsdc)}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(product.priceUsdc / solPrice).format(4)} SOL",
                    style = MaterialTheme.typography.titleMedium,
                    color = solPriceColor,
                    modifier = Modifier.scale(solPriceScale)
                )
            }
        }

        // Selectors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 470.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)
        ) {
            productItem?.let { product ->
                // Stable DropdownMenu for size, opens downward
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (product.type == "rwi") {
                        var sizeExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { sizeExpanded = true }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = selectedSize ?: "Size",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .offset(y = 48.dp)
                            ) {
                                DropdownMenu(
                                    expanded = sizeExpanded,
                                    onDismissRequest = { sizeExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    sizes?.forEach { size ->
                                        val variant = product.variants?.mapNotNull { (key, value) ->
                                            val variantMap = value as? Map<*, *>
                                            val s = variantMap?.get("size") as? String
                                            val qty = when (val rawQty = variantMap?.get("quantity")) {
                                                is String -> rawQty.toIntOrNull() ?: 0
                                                is Number -> rawQty.toInt()
                                                else -> 0
                                            }
                                            if (s == size) mapOf("size" to s, "quantity" to qty) else null
                                        }?.firstOrNull()
                                        val isOutOfStock = variant?.get("quantity") == 0
                                        DropdownMenuItem(
                                            text = { Text(size) },
                                            onClick = {
                                                if (!isOutOfStock) {
                                                    selectedSize = size
                                                    sizeExpanded = false
                                                }
                                            },
                                            enabled = !isOutOfStock,
                                            modifier = Modifier.background(
                                                if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant
                                                else MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        var colorExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { colorExpanded = true }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = selectedColor ?: "Color",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            DropdownMenu(
                                expanded = colorExpanded,
                                onDismissRequest = { colorExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                colors?.forEach { color ->
                                    val variant = product.variants?.mapNotNull { (key, value) ->
                                        val variantMap = value as? Map<*, *>
                                        val c = variantMap?.get("color") as? String
                                        val qty = when (val rawQty = variantMap?.get("quantity")) {
                                            is String -> rawQty.toIntOrNull() ?: 0
                                            is Number -> rawQty.toInt()
                                            else -> 0
                                        }
                                        if (c == color) mapOf("color" to c, "quantity" to qty) else null
                                    }?.firstOrNull()
                                    val isOutOfStock = variant?.get("quantity") == 0
                                    DropdownMenuItem(
                                        text = { Text(color) },
                                        onClick = {
                                            if (!isOutOfStock) {
                                                selectedColor = color
                                                colorExpanded = false
                                            }
                                        },
                                        enabled = !isOutOfStock,
                                        modifier = Modifier.background(
                                            if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Quantity selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically // CHANGED: Added vertical alignment
                ) {
                    IconButton(
                        onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                        enabled = selectedQuantity > 1,
                        modifier = Modifier.size(48.dp) // CHANGED: Fixed size for consistency
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center // CHANGED: Center content
                        ) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selectedQuantity > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = selectedQuantity.toString(),
                        style = MaterialTheme.typography.titleLarge, // CHANGED: Use titleLarge for consistent alignment
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center // CHANGED: Center text
                    )
                    IconButton(
                        onClick = { if (selectedQuantity < maxQuantity) selectedQuantity++ },
                        enabled = selectedQuantity < maxQuantity,
                        modifier = Modifier.size(48.dp) // CHANGED: Fixed size for consistency
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center // CHANGED: Center content
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selectedQuantity < maxQuantity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val db = FirebaseFirestore.getInstance()
                            productItem?.let { product ->
                                val cartItemId = if (product.type == "rwi") {
                                    "${product.productId}-${selectedSize}-${selectedColor}"
                                } else {
                                    product.productId
                                }
                                try {
                                    val cartItem = mapOf(
                                        "productId" to product.productId,
                                        "storeId" to product.storeId,
                                        "sellerId" to product.sellerId,
                                        "name" to product.name,
                                        "priceUsdc" to product.priceUsdc,
                                        "quantity" to selectedQuantity,
                                        "color" to (if (product.type == "rwi") selectedColor else null),
                                        "size" to (if (product.type == "rwi") selectedSize else null),
                                        "imageUrl" to (product.imageUrl.ifEmpty { "/img/examples/default.jpg" }),
                                        "addedAt" to Instant.now().toString(),
                                        "walletId" to walletId
                                    )
                                    db.collection("users")
                                        .document(walletId)
                                        .collection("cart")
                                        .document(cartItemId)
                                        .set(cartItem)
                                        .await()
                                    db.collection("users")
                                        .document(walletId)
                                        .collection("marketplaceClicks")
                                        .add(
                                            mapOf(
                                                "productId" to product.productId,
                                                "timestamp" to Instant.now().toString()
                                            )
                                        ).await()
                                    showCartNotification = true
                                    Log.d("ProductScreen", "Added to cart: $cartItem")
                                } catch (e: Exception) {
                                    Log.e("ProductScreen", "Error adding to cart: ${e.message}")
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Add to Bag",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Add to Bag"
                        )
                    }
                }
            } ?: run {
                if (!isLoading) {
                    Text(
                        text = "Product not found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Info FAB (bottom-left)
        FloatingActionButton(
            onClick = { showInfoPopup = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(bottom = 24.dp, start = 24.dp)
                .size(56.dp)
                .align(Alignment.BottomStart)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Product Info"
            )
        }

        // Info Popup
        if (showInfoPopup) {
            AlertDialog(
                onDismissRequest = { showInfoPopup = false },
                title = null,
                text = {
                    Column {
                        productItem?.let { product ->
                            Text(
                                text = "Name: ${product.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Description: ${product.description}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Categories: ${product.categories.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Store ID: ${product.storeId}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoPopup = false }) {
                        Text("Close")
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // FAB Menu with purple buttons
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
        ) {
            val scale by animateFloatAsState(if (isFabMenuExpanded) 1f else 0f)
            if (isFabMenuExpanded) {
                FloatingActionButton(
                    onClick = { navigateBack() },
                    containerColor = Color(0xFF4D455D),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(width = 120.dp, height = 48.dp)
                        .scale(scale)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { navigateToAffiliates() },
                    containerColor = Color(0xFF4D455D),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(width = 120.dp, height = 48.dp)
                        .scale(scale)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = "Affiliates"
                        )
                        Text(
                            text = "Affiliates",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { /* Placeholder for Profile action */ },
                    containerColor = Color(0xFF4D455D),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(width = 120.dp, height = 48.dp)
                        .scale(scale)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Face,
                            contentDescription = "Profile"
                        )
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            FloatingActionButton(
                onClick = { isFabMenuExpanded = !isFabMenuExpanded },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isFabMenuExpanded) Icons.Outlined.Close else Icons.Outlined.Add,
                    contentDescription = if (isFabMenuExpanded) "Close Menu" else "Open Menu"
                )
            }
        }

        // Cart Notification Popup
        AnimatedVisibility(
            visible = showCartNotification,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 80.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { navigateToCart() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Cart Added",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Item added to cart!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            LaunchedEffect(showCartNotification) {
                kotlinx.coroutines.delay(2000)
                showCartNotification = false
            }
        }
    }
}

// Extension function to format double to specified decimal places
fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Preview(showBackground = true)
@Composable
fun ProductScreenPreview() {
    F4cetMobileTheme {
        ProductScreen(
            userProfile = User.Profile(name = "Test User", avatar = "", email = "", nfts = emptyList()),
            productId = "cxSJ1gj5d2Q7bduMsTEC",
            navigateBack = {},
            navigateToAffiliates = {},
            navigateToStore = {},
            navigateToProduct = {},
            navigateToCart = {}
        )
    }
}