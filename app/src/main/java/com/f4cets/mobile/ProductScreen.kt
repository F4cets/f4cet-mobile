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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.f4cets.mobile.model.MarketplaceItem
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant

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
    var quantity by remember { mutableStateOf(1) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showCartNotification by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"
    val listState = rememberLazyListState()
    val sliderOffset = remember { mutableStateOf(0f) }
    val sliderHeight = 200.dp
    val minOffset = 0f
    val maxOffset = sliderHeight.value

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
                    val variants = productDoc.get("variants") as? Map<String, Any>
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
                        variants = variants
                    )
                    Log.d("ProductScreen", "Product fetched: id=$productId, name=$name")

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

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            content = { innerPadding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Store banner with parallax and fade
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            val scrollOffset = listState.firstVisibleItemScrollOffset * 0.10f
                            val bannerUrl = storeItem?.imageUrl ?: ""
                            if (bannerUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Store Banner",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = -scrollOffset.dp)
                                        .height(300.dp),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.bgf)
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.bgf),
                                    contentDescription = "Default Banner",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = -scrollOffset.dp)
                                        .height(300.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0f to Color.Transparent,
                                                1f to Color(0xFFFDFAF0)
                                            ),
                                            startY = 300f,
                                            endY = 600f
                                        )
                                    )
                            )
                            // Floating product image
                            productItem?.let { product ->
                                if (product.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = "Product Image for ${product.name}",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(250.dp, 200.dp)
                                            .zIndex(1f)
                                            .offset(y = (-50).dp),
                                        contentScale = ContentScale.Fit,
                                        error = painterResource(id = R.drawable.bgf)
                                    )
                                }
                            }
                        }
                    }

                    // Product details
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            productItem?.let { product ->
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$${product.priceUsdc}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "10% OFF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
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
                                    text = product.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Categories: ${product.categories.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Size selector for RWI
                                if (product.type == "rwi") {
                                    val sizes = product.variants?.get("size") as? List<String> ?: listOf("9", "9.5", "10", "11")
                                    Text(
                                        text = "Size",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        sizes.forEach { size ->
                                            FilterChip(
                                                selected = selectedSize == size,
                                                onClick = { selectedSize = size },
                                                label = { Text(size) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }

                                    // Color selector for RWI
                                    val colors = product.variants?.get("color") as? List<String> ?: listOf("Blue", "Red")
                                    Text(
                                        text = "Colour",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        colors.forEach { color ->
                                            FilterChip(
                                                selected = selectedColor == color,
                                                onClick = { selectedColor = color },
                                                label = { Text(color) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            )
                                        }
                                    }
                                }

                                // Quantity selector for digital or RWI
                                Text(
                                    text = "Quantity",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { if (quantity > 1) quantity-- },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text("-")
                                    }
                                    Text(
                                        text = quantity.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = { quantity++ },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text("+")
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
                    }
                }
            }
        )

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

        // Add to Bag Slider
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
                    .offset(y = (sliderHeight.value - offset).dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { sliderOffset.value = minOffset },
                            onVerticalDrag = { change: PointerInputChange, dragAmount: Float -> // CHANGED: Corrected to onVerticalDrag with explicit types
                                sliderOffset.value = (sliderOffset.value + dragAmount).coerceIn(minOffset, maxOffset)
                            },
                            onDragEnd = {
                                if (sliderOffset.value >= maxOffset * 0.8f) {
                                    coroutineScope.launch {
                                        val db = FirebaseFirestore.getInstance()
                                        productItem?.let { product ->
                                            db.collection("users")
                                                .document(walletId)
                                                .collection("cart")
                                                .add(
                                                    mapOf(
                                                        "productId" to product.productId,
                                                        "quantity" to quantity,
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
                                sliderOffset.value = minOffset
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .size(60.dp, 40.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Add to Bag",
                        tint = Color.Black
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