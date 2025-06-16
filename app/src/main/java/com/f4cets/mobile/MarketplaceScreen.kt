package com.f4cets.mobile

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
fun MarketplaceScreen(
    userProfile: User.Profile?,
    navigateBack: () -> Unit,
    navigateToAffiliates: () -> Unit,
    navigateToStore: (String) -> Unit,
    navigateToProduct: (String) -> Unit
) {
    var marketplaceItems by remember { mutableStateOf<List<MarketplaceItem>>(emptyList()) }
    var storeItems by remember { mutableStateOf<List<StoreItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("All") }
    var selectedViewMode by remember { mutableStateOf("All") }
    var priceRangeMin by remember { mutableStateOf(0f) }
    var priceRangeMax by remember { mutableStateOf(1000f) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isFilterDialogOpen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"
    val context = LocalContext.current

    val categories = listOf(
        "Accessories", "Art & Collectibles", "Baby & Toddler", "Beauty", "Books, Movies & Music",
        "Clothing", "Craft Supplies", "Digital Goods", "Digital Services", "Ebooks", "EGames",
        "Electronics", "Fitness & Nutrition", "Food & Drinks", "Home & Living", "Jewelry",
        "Luggage & Bags", "NFTs", "Pet Supplies", "Private Access Groups", "Shoes", "Software",
        "Sporting Goods", "Toys & Games"
    )

    LaunchedEffect(selectedCategory, selectedType, selectedViewMode) {
        isLoading = true
        coroutineScope.launch {
            val db = FirebaseFirestore.getInstance()

            // Fetch products
            var productQuery = db.collection("products")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt")
            if (selectedCategory.isNotEmpty()) {
                productQuery = productQuery.whereArrayContains("categories", selectedCategory)
            }
            if (selectedType != "All") {
                productQuery = productQuery.whereEqualTo("type", selectedType.lowercase())
            }
            Log.d("MarketplaceScreen", "Fetching products: collection=products, isActive=true, category=$selectedCategory, type=$selectedType")
            try {
                val productDocuments = productQuery.get().await()
                val products = productDocuments.documents.mapNotNull { doc ->
                    val productId = doc.id
                    val name = doc.get("name") as? String ?: return@mapNotNull null
                    val description = doc.get("description") as? String ?: ""
                    val priceUsdc = (doc.get("price") as? Number)?.toDouble() ?: 0.0
                    val selectedImage = doc.get("selectedImage") as? String ?: ""
                    val imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    val imageUrl = selectedImage.ifEmpty { imageUrls.firstOrNull() ?: "" }
                    val cryptoBackOffer = doc.get("cryptoBackOffer") as? String ?: "Upto 5% Crypto Cashback"
                    val storeId = doc.get("storeId") as? String ?: ""
                    val sellerId = doc.get("sellerId") as? String ?: ""
                    val type = doc.get("type") as? String ?: "digital"
                    val categories = doc.get("categories") as? List<String> ?: emptyList()
                    Log.d("MarketplaceScreen", "Product: id=$productId, name=$name, price=$priceUsdc")
                    MarketplaceItem(
                        productId = productId,
                        name = name,
                        description = description,
                        priceUsdc = priceUsdc,
                        imageUrl = imageUrl,
                        cryptoBackOffer = cryptoBackOffer,
                        storeId = storeId,
                        sellerId = sellerId,
                        type = type,
                        categories = categories
                    )
                }
                marketplaceItems = products
            } catch (e: Exception) {
                Log.e("MarketplaceScreen", "Product fetch error: ${e.message}")
            }

            // Fetch stores
            var storeQuery = db.collection("stores")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt")
            if (selectedCategory.isNotEmpty()) {
                storeQuery = storeQuery.whereArrayContains("categories", selectedCategory)
            }
            Log.d("MarketplaceScreen", "Fetching stores: collection=stores, isActive=true, category=$selectedCategory")
            try {
                val storeDocuments = storeQuery.get().await()
                val stores = storeDocuments.documents.mapNotNull { doc ->
                    val storeId = doc.id
                    val name = doc.get("name") as? String ?: return@mapNotNull null
                    val description = doc.get("description") as? String ?: ""
                    val thumbnailUrl = doc.get("thumbnailUrl") as? String ?: ""
                    val sellerId = doc.get("sellerId") as? String ?: ""
                    val categories = doc.get("categories") as? List<String> ?: emptyList()
                    Log.d("MarketplaceScreen", "Store: id=$storeId, name=$name")
                    StoreItem(
                        storeId = storeId,
                        name = name,
                        description = description,
                        thumbnailUrl = thumbnailUrl,
                        sellerId = sellerId,
                        categories = categories
                    )
                }
                storeItems = stores
            } catch (e: Exception) {
                Log.e("MarketplaceScreen", "Store fetch error: ${e.message}")
            }
            isLoading = false
        }
    }

    val filteredItems = when (selectedViewMode) {
        "Stores" -> storeItems.filter { store ->
            searchQuery.trim().isEmpty() || store.name.lowercase().contains(searchQuery.trim().lowercase())
        }.map { Item.Store(it) }
        "Products" -> marketplaceItems.filter { product ->
            (searchQuery.trim().isEmpty() || product.name.lowercase().contains(searchQuery.trim().lowercase())) &&
                    product.priceUsdc >= priceRangeMin && product.priceUsdc <= priceRangeMax
        }.map { Item.Product(it) }
        else -> (storeItems.map { Item.Store(it) } + marketplaceItems.filter { product ->
            product.priceUsdc >= priceRangeMin && product.priceUsdc <= priceRangeMax
        }.map { Item.Product(it) }).filter { item ->
            val name = when (item) {
                is Item.Store -> item.store.name
                is Item.Product -> item.product.name
            }
            searchQuery.trim().isEmpty() || name.lowercase().contains(searchQuery.trim().lowercase())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background image
                Image(
                    painter = painterResource(id = R.drawable.bgf),
                    contentDescription = "Background Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Avatar (top left, same spacing)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 24.dp, start = 24.dp)
                        .size(56.dp)
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

                // Cart button (top right, same spacing)
                FloatingActionButton(
                    onClick = { /* Placeholder for shopping cart action */ },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Shopping Cart"
                    )
                }

                // Search bar and filter (unchanged position)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp, start = 16.dp, end = 24.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 8.dp)
                    ) {
                        IconButton(onClick = { isFilterDialogOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.List,
                                contentDescription = "Open Filters",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Search Marketplace") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            focusedIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                }

                // Product grid (infinite scroll)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 192.dp, start = 16.dp, end = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            ElevatedCard(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    }
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
                                            if (item.store.thumbnailUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = item.store.thumbnailUrl,
                                                    contentDescription = "Store Thumbnail for ${item.store.name}",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Text(
                                                    text = item.store.name.take(1),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = item.store.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.store.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        is Item.Product -> {
                                            if (item.product.imageUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = item.product.imageUrl,
                                                    contentDescription = "Product Image for ${item.product.name}",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                                                    contentScale = ContentScale.Fit
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
                    if (isLoading) {
                        item(span = { GridItemSpan(2) }) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // FAB menu (bottom right, same spacing)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                }

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
                                Text(
                                    text = "Filter Options",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "View Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FilterChip(
                                        selected = selectedViewMode == "All",
                                        onClick = { selectedViewMode = "All" },
                                        label = { Text("All") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                    FilterChip(
                                        selected = selectedViewMode == "Stores",
                                        onClick = { selectedViewMode = "Stores" },
                                        label = { Text("Stores") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                    FilterChip(
                                        selected = selectedViewMode == "Products",
                                        onClick = { selectedViewMode = "Products" },
                                        label = { Text("Products") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
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
                                    startThumb = { // CHANGED: Smaller thumb size
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    },
                                    endThumb = { // CHANGED: Smaller thumb size
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
    )
}

@Preview(showBackground = true)
@Composable
fun MarketplaceScreenPreview() {
    F4cetMobileTheme {
        MarketplaceScreen(
            userProfile = User.Profile(name = "Test User", avatar = "", email = "", nfts = emptyList()),
            navigateBack = {},
            navigateToAffiliates = {},
            navigateToStore = {},
            navigateToProduct = {}
        )
    }
}