package com.f4cets.mobile

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.f4cets.mobile.model.MarketplaceItem
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
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
    var storeItem by remember { mutableStateOf<MarketplaceItem?>(null) }
    var selectedSize by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showCartNotification by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"
    val sliderOffset = remember { mutableStateOf(0f) }
    val sliderHeight = 200.dp
    val minOffset = 0f
    val maxOffset = sliderHeight.value
    var sizes by remember { mutableStateOf<List<String>?>(null) }
    var colors by remember { mutableStateOf<List<String>?>(null) }
    val solPrice = 147.48f // Placeholder; replace with dynamic value later

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
                    val imageUrls = productDoc.get("imageUrls") as? List<String> ?: emptyList()
                    val imageUrl = selectedImage.ifEmpty { imageUrls.firstOrNull() ?: "" }
                    val cryptoBackOffer = productDoc.getString("cryptoBackOffer") ?: "Upto 5% Crypto Cashback"
                    val storeId = productDoc.getString("storeId") ?: ""
                    val sellerId = productDoc.getString("sellerId") ?: ""
                    val type = productDoc.getString("type") ?: "digital"
                    val categories = productDoc.get("categories") as? List<String> ?: emptyList()
                    val variants = productDoc.get("variants") as? List<Map<String, Any>> ?: emptyList()
                    // CHANGED: Parse variants array for size, color, and quantity
                    val variantList = variants.mapNotNull { variantMap ->
                        val size = variantMap["size"] as? String
                        val color = variantMap["color"] as? String
                        val qty = (variantMap["quantity"] as? String)?.toIntOrNull() ?: (variantMap["quantity"] as? Number)?.toInt() ?: 0
                        if (qty > 0) mapOf("size" to size, "color" to color, "quantity" to qty) else null
                    }
                    sizes = variantList.mapNotNull { it["size"] as? String }.distinct()
                    colors = variantList.mapNotNull { it["color"] as? String }.distinct()
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
                    Log.d("ProductScreen", "Product fetched: id=$productId, name=$name, variants=$variantList")

                    // Fetch store banner
                    if (storeId.isNotEmpty()) {
                        val storeDoc = db.collection("stores")
                            .document(storeId)
                            .get()
                            .await()
                        val bannerUrl = storeDoc.getString("bannerUrl") ?: ""
                        storeItem = MarketplaceItem(productId = storeId, imageUrl = bannerUrl)
                        Log.d("ProductScreen", "Store banner fetched: id=$storeId, bannerUrl=$bannerUrl")
                    }
                } else {
                    Log.e("ProductScreen", "Product not found: id=$productId")
                }
            } catch (e: Exception) {
                Log.e("ProductScreen", "Product fetch error: ${e.message}")
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background with bgp.png
        Image(
            painter = painterResource(id = R.drawable.bgp),
            contentDescription = "Product Background with Bag",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Product name above image
        productItem?.let { product ->
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            )
        }

        // Elevated card with product image (lower position, drop shadow)
        productItem?.let { product ->
            if (product.imageUrl.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 120.dp)
                        .size(300.dp, 300.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = "Product Image for ${product.name}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        error = painterResource(id = R.drawable.bgf)
                    )
                }
            }
        }

        // Product details in non-scrollable Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 350.dp, start = 16.dp, end = 16.dp, bottom = 80.dp)
        ) {
            productItem?.let { product ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "$${product.priceUsdc}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(product.priceUsdc / solPrice).format(4)} SOL",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Size selector for RWI (vertical on left)
                if (product.type == "rwi") {
                    Text(
                        text = "Size",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizes?.forEach { size ->
                            val variant = product.variants?.mapNotNull { (key, value) ->
                                val variantMap = value as? Map<*, *>
                                val s = variantMap?.get("size") as? String
                                val qty = (variantMap?.get("quantity") as? String)?.toIntOrNull() ?: (variantMap?.get("quantity") as? Number)?.toInt() ?: 0
                                if (s == size) mapOf("size" to s, "quantity" to qty) else null
                            }?.firstOrNull()
                            val isOutOfStock = variant?.get("quantity") == 0
                            FilterChip(
                                selected = selectedSize == size && !isOutOfStock,
                                onClick = { if (!isOutOfStock) selectedSize = size },
                                label = { Text(size) },
                                enabled = !isOutOfStock,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (sizes?.all { size ->
                                product.variants?.mapNotNull { (key, value) ->
                                    val variantMap = value as? Map<*, *>
                                    val s = variantMap?.get("size") as? String
                                    val qty = (variantMap?.get("quantity") as? String)?.toIntOrNull() ?: (variantMap?.get("quantity") as? Number)?.toInt() ?: 0
                                    if (s == size) qty else null
                                }?.firstOrNull() == 0
                            } == true) {
                            Text(
                                text = "Out of Stock",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Color selector for RWI (vertical on right)
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp)
                            .align(Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors?.forEach { color ->
                            val variant = product.variants?.mapNotNull { (key, value) ->
                                val variantMap = value as? Map<*, *>
                                val c = variantMap?.get("color") as? String
                                val qty = (variantMap?.get("quantity") as? String)?.toIntOrNull() ?: (variantMap?.get("quantity") as? Number)?.toInt() ?: 0
                                if (c == color) mapOf("color" to c, "quantity" to qty) else null
                            }?.firstOrNull()
                            val isOutOfStock = variant?.get("quantity") == 0
                            FilterChip(
                                selected = selectedColor == color && !isOutOfStock,
                                onClick = { if (!isOutOfStock) selectedColor = color },
                                label = { Text(color) },
                                enabled = !isOutOfStock,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        if (colors?.all { color ->
                                product.variants?.mapNotNull { (key, value) ->
                                    val variantMap = value as? Map<*, *>
                                    val c = variantMap?.get("color") as? String
                                    val qty = (variantMap?.get("quantity") as? String)?.toIntOrNull() ?: (variantMap?.get("quantity") as? Number)?.toInt() ?: 0
                                    if (c == color) qty else null
                                }?.firstOrNull() == 0
                            } == true) {
                            Text(
                                text = "Out of Stock",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Store: ${product.storeId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable {
                        if (product.storeId.isNotEmpty()) {
                            navigateToStore(product.storeId)
                        }
                    }
                )
                Text(
                    text = "Categories: ${product.categories.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

        // Overlay fixed UI elements
        // Avatar (top-left)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp)
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
            onClick = { /* Placeholder for shopping cart action */ },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .size(56.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = "Shopping Cart"
            )
        }

        // FAB Menu (bottom-right)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            val scale by animateFloatAsState(if (isFabMenuExpanded) 1f else 0f)
            if (isFabMenuExpanded) {
                FloatingActionButton(
                    onClick = { navigateBack() },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
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
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
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
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
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

        // Add to Bag Slider (adjusted for visibility)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sliderHeight)
        ) {
            val offset by animateFloatAsState(
                targetValue = sliderOffset.value,
                animationSpec = tween(durationMillis = 200)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sliderHeight)
                    .offset(y = -(sliderHeight.value - offset).dp) // CHANGED: Adjusted offset to move upward from below
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { sliderOffset.value = minOffset },
                            onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                                sliderOffset.value = (sliderOffset.value + dragAmount).coerceIn(minOffset, maxOffset)
                            },
                            onDragEnd = {
                                if (sliderOffset.value >= maxOffset * 0.8f) {
                                    coroutineScope.launch {
                                        val db = FirebaseFirestore.getInstance()
                                        productItem?.let { product ->
                                            val selectedVariant = product.variants?.mapNotNull { (key, value) ->
                                                val variantMap = value as? Map<*, *>
                                                val size = variantMap?.get("size") as? String
                                                val color = variantMap?.get("color") as? String
                                                val qty = (variantMap?.get("quantity") as? String)?.toIntOrNull() ?: (variantMap?.get("quantity") as? Number)?.toInt() ?: 0
                                                if (size == selectedSize && color == selectedColor && qty > 0) qty else null
                                            }?.firstOrNull()
                                            if (selectedVariant != null && selectedVariant > 0) {
                                                db.collection("users")
                                                    .document(walletId)
                                                    .collection("cart")
                                                    .add(
                                                        mapOf(
                                                            "productId" to product.productId,
                                                            "quantity" to 1,
                                                            "size" to selectedSize,
                                                            "color" to selectedColor,
                                                            "timestamp" to Instant.now().toString()
                                                        )
                                                    ).await()
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
                                            }
                                        }
                                    }
                                }
                                sliderOffset.value = minOffset
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(80.dp, 60.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add to Bag",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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