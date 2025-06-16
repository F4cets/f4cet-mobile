package com.f4cets.mobile

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.f4cets.mobile.model.Item
import com.f4cets.mobile.model.MarketplaceItem
import com.f4cets.mobile.model.StoreItem
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    userProfile: User.Profile?,
    storeId: String,
    navigateBack: () -> Unit,
    navigateToAffiliates: () -> Unit,
    navigateToStore: (String) -> Unit,
    navigateToProduct: (String) -> Unit
) {
    var marketplaceItems by remember { mutableStateOf<List<MarketplaceItem>>(emptyList()) }
    var storeItem by remember { mutableStateOf<StoreItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("All") }
    var priceRangeMin by remember { mutableStateOf(0f) }
    var priceRangeMax by remember { mutableStateOf(1000f) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isFilterDialogOpen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val categories = listOf(
        "Accessories", "Art & Collectibles", "Baby & Toddler", "Beauty", "Books, Movies & Music",
        "Clothing", "Craft Supplies", "Digital Goods", "Digital Services", "Ebooks", "EGames",
        "Electronics", "Fitness & Nutrition", "Food & Drinks", "Home & Living", "Jewelry",
        "Luggage & Bags", "NFTs", "Pet Supplies", "Private Access Groups", "Shoes", "Software",
        "Sporting Goods", "Toys & Games"
    )

    LaunchedEffect(storeId, selectedCategory, selectedType) {
        if (storeId.isEmpty()) {
            Log.e("StoreScreen", "Store ID is empty")
            return@LaunchedEffect
        }
        isLoading = true
        coroutineScope.launch {
            val db = FirebaseFirestore.getInstance()

            // Fetch store details including bannerUrl
            Log.d("StoreScreen", "Fetching store: id=$storeId")
            try {
                val storeDoc = db.collection("stores")
                    .document(storeId)
                    .get()
                    .await()
                if (storeDoc.exists()) {
                    val name = storeDoc.getString("name") ?: ""
                    val description = storeDoc.getString("description") ?: ""
                    val thumbnailUrl = storeDoc.getString("thumbnailUrl") ?: ""
                    val bannerUrl = storeDoc.getString("bannerUrl") ?: ""
                    val sellerId = storeDoc.getString("sellerId") ?: ""
                    val storeCategories = storeDoc.get("categories") as? List<String> ?: emptyList()
                    storeItem = StoreItem(
                        storeId = storeId,
                        name = name,
                        description = description,
                        thumbnailUrl = thumbnailUrl,
                        bannerUrl = bannerUrl,
                        sellerId = sellerId,
                        categories = storeCategories
                    )
                    Log.d("StoreScreen", "Store fetched: id=$storeId, name=$name, bannerUrl=$bannerUrl")
                } else {
                    Log.e("StoreScreen", "Store not found: id=$storeId")
                }
            } catch (e: Exception) {
                Log.e("StoreScreen", "Store fetch error: ${e.message}")
            }

            // Fetch products for this store
            var productQuery = db.collection("products")
                .whereEqualTo("isActive", true)
                .whereEqualTo("storeId", storeId)
                .orderBy("createdAt")
            if (selectedCategory.isNotEmpty()) {
                productQuery = productQuery.whereArrayContains("categories", selectedCategory)
            }
            if (selectedType != "All") {
                productQuery = productQuery.whereEqualTo("type", selectedType.lowercase())
            }
            Log.d("StoreScreen", "Fetching products: collection=products, storeId=$storeId, isActive=true, category=$selectedCategory, type=$selectedType")
            try {
                val productDocuments = productQuery.get().await()
                val products = productDocuments.documents.mapNotNull { doc ->
                    val productId = doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val priceUsdc = (doc.get("price") as? Number)?.toDouble() ?: 0.0
                    val selectedImage = doc.getString("selectedImage") ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    val imageUrl = selectedImage.ifEmpty { imageUrls.firstOrNull() ?: "" }
                    val cryptoBackOffer = doc.getString("cryptoBackOffer") ?: "Upto 5% Crypto Cashback"
                    val productStoreId = doc.getString("storeId") ?: ""
                    val sellerId = doc.getString("sellerId") ?: ""
                    val type = doc.getString("type") ?: "digital"
                    val productCategories = doc.get("categories") as? List<String> ?: emptyList()
                    Log.d("StoreScreen", "Product: id=$productId, name=$name, price=$priceUsdc")
                    MarketplaceItem(
                        productId = productId,
                        name = name,
                        description = description,
                        priceUsdc = priceUsdc,
                        imageUrl = imageUrl,
                        cryptoBackOffer = cryptoBackOffer,
                        storeId = productStoreId,
                        sellerId = sellerId,
                        type = type,
                        categories = productCategories
                    )
                }
                marketplaceItems = products
            } catch (e: Exception) {
                Log.e("StoreScreen", "Product fetch error: ${e.message}")
            }
            isLoading = false
        }
    }

    val filteredItems = marketplaceItems.filter { product ->
        (searchQuery.trim().isEmpty() || product.name.lowercase().contains(searchQuery.trim().lowercase())) &&
                product.priceUsdc >= priceRangeMin && product.priceUsdc <= priceRangeMax
    }.map { Item.Product(it) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFFFDFAF0),
            content = { innerPadding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Banner image with parallax and fade
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            val scrollOffset = listState.firstVisibleItemScrollOffset * 0.10f
                            val bannerUrl = storeItem?.bannerUrl ?: ""
                            if (bannerUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = bannerUrl,
                                    contentDescription = "Store Banner for ${storeItem?.name}",
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
                            // Gradient fade from transparent at 60% to #FDFAF0 at 100%
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
                        }
                    }

                    // Product grid
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredItems.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { item ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(bottom = 8.dp)
                                            ) {
                                                ElevatedCard(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            coroutineScope.launch {
                                                                val db = FirebaseFirestore.getInstance()
                                                                when (item) {
                                                                    is Item.Store -> {
                                                                        db.collection("users")
                                                                            .document(walletId)
                                                                            .collection("marketplaceClicks")
                                                                            .add(
                                                                                mapOf(
                                                                                    "storeId" to item.store.storeId,
                                                                                    "timestamp" to Instant.now().toString()
                                                                                )
                                                                            ).await()
                                                                        navigateToStore(item.store.storeId)
                                                                    }
                                                                    is Item.Product -> {
                                                                        db.collection("users")
                                                                            .document(walletId)
                                                                            .collection("marketplaceClicks")
                                                                            .add(
                                                                                mapOf(
                                                                                    "productId" to item.product.productId,
                                                                                    "timestamp" to Instant.now().toString()
                                                                                )
                                                                            ).await()
                                                                        navigateToProduct(item.product.productId)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(8.dp),
                                                        verticalArrangement = Arrangement.SpaceBetween,
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        when (item) {
                                                            is Item.Store -> {
                                                                // Skip stores, as this is store-specific
                                                            }
                                                            is Item.Product -> {
                                                                if (item.product.imageUrl.isNotEmpty()) {
                                                                    AsyncImage(
                                                                        model = item.product.imageUrl,
                                                                        contentDescription = "Product Image for ${item.product.name}",
                                                                        modifier = Modifier
                                                                            .fillMaxWidth()
                                                                            .aspectRatio(1f)
                                                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                                                        contentScale = ContentScale.Crop
                                                                    )
                                                                } else {
                                                                    Text(
                                                                        text = item.product.name.take(1),
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        textAlign = TextAlign.Center
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                Text(
                                                                    text = item.product.name,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = TextAlign.Center,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = item.product.description,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = TextAlign.Center,
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "${item.product.priceUsdc} USDC",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        // Add empty Box if pair has only one item
                                        if (pair.size == 1) {
                                            Box(modifier = Modifier.weight(1f))
                                        }
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

        // Filter/Search FAB
        FloatingActionButton(
            onClick = { isFilterDialogOpen = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp, start = 16.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Filter/Search"
            )
        }

        // Filter Dialog
        if (isFilterDialogOpen) {
            AlertDialog(
                onDismissRequest = { isFilterDialogOpen = false },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "Search ${storeItem?.name ?: "Store"}",
                                    style = MaterialTheme.typography.labelSmall // CHANGED: Smaller placeholder font
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                focusedIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(28.dp),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        )
                        Text(
                            text = "Product Type",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilterChip(
                                selected = selectedType == "All",
                                onClick = { selectedType = "All" },
                                label = { Text("All Types") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = selectedType == "RWI",
                                onClick = { selectedType = "RWI" },
                                label = { Text("RWI") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = selectedType == "Digital",
                                onClick = { selectedType = "Digital" },
                                label = { Text("Digital") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ExposedDropdownMenuBox(
                            expanded = isCategoryMenuExpanded,
                            onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded }
                        ) {
                            TextField(
                                value = selectedCategory.ifEmpty { "All" },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = isCategoryMenuExpanded,
                                onDismissRequest = { isCategoryMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        selectedCategory = ""
                                        isCategoryMenuExpanded = false
                                    }
                                )
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            selectedCategory = category
                                            isCategoryMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Price Range: $${priceRangeMin.toInt()} - $${priceRangeMax.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        RangeSlider(
                            value = priceRangeMin..priceRangeMax,
                            onValueChange = { range ->
                                priceRangeMin = range.start
                                priceRangeMax = range.endInclusive
                            },
                            valueRange = 0f..1000f,
                            steps = 100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            startThumb = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            },
                            endThumb = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isFilterDialogOpen = false }) {
                                Text("Cancel")
                            }
                            TextButton(onClick = { isFilterDialogOpen = false }) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StoreScreenPreview() {
    F4cetMobileTheme {
        StoreScreen(
            userProfile = User.Profile(name = "Test User", avatar = "", email = "", nfts = emptyList()),
            storeId = "mixed-emotions-0f9abb6e",
            navigateBack = {},
            navigateToAffiliates = {},
            navigateToStore = {},
            navigateToProduct = {}
        )
    }
}