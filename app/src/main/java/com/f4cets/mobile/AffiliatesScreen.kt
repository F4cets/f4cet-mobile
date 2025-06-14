package com.f4cets.mobile

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import com.f4cets.mobile.model.AffiliateItem
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant

@Composable
fun AffiliatesScreen(
    userProfile: User.Profile?,
    navigateBack: () -> Unit,
    navigateToMarketplace: () -> Unit
) {
    var affiliateItems by remember { mutableStateOf<List<AffiliateItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
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

    LaunchedEffect(selectedCategory) {
        isLoading = true
        coroutineScope.launch {
            val db = FirebaseFirestore.getInstance()
            var query = db.collection("affiliates")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt")
            if (selectedCategory.isNotEmpty()) {
                query = query.whereArrayContains("categories", selectedCategory)
                Log.d("AffiliatesScreen", "Filtering by category: $selectedCategory")
            }
            Log.d("AffiliatesScreen", "Fetching affiliates: collection=affiliates, isActive=true, category=${selectedCategory.ifEmpty { "all" }}")
            try {
                val affiliateDocuments = query.get().await()
                val affiliates = affiliateDocuments.documents.mapNotNull { doc ->
                    val affiliateId = doc.id
                    val name = doc.get("name") as? String ?: return@mapNotNull null
                    val logoUrl = doc.get("logoUrl") as? String ?: ""
                    val cryptoBackOffer = doc.get("cryptoBackOffer") as? String ?: "Upto 5% Crypto Cashback"
                    val createdAt = doc.get("createdAt")?.toString() ?: "null"
                    Log.d("AffiliatesScreen", "Document: id=$affiliateId, name=$name, createdAt=$createdAt")
                    AffiliateItem(affiliateId, name, logoUrl, cryptoBackOffer)
                }
                affiliateItems = affiliates
                Log.d("AffiliatesScreen", "Fetch complete: loaded=${affiliates.size}, total=${affiliateItems.size}")
            } catch (e: Exception) {
                Log.e("AffiliatesScreen", "Fetch error: ${e.message}")
            }
            isLoading = false
        }
    }

    val filteredAffiliates = if (searchQuery.trim().isEmpty()) {
        affiliateItems
    } else {
        affiliateItems.filter { it.name.lowercase().contains(searchQuery.trim().lowercase()) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bgf),
                    contentDescription = "Background Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp, start = 16.dp, end = 24.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .padding(end = 8.dp)
                        ) {
                            IconButton(
                                onClick = { isCategoryMenuExpanded = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.List,
                                    contentDescription = "Filter by Category",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = isCategoryMenuExpanded,
                            onDismissRequest = { isCategoryMenuExpanded = false },
                            modifier = Modifier
                                .width(200.dp)
                                .heightIn(max = 400.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All") },
                                onClick = {
                                    selectedCategory = ""
                                    isCategoryMenuExpanded = false
                                },
                                trailingIcon = {
                                    if (selectedCategory.isEmpty()) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        isCategoryMenuExpanded = false
                                    },
                                    trailingIcon = {
                                        if (selectedCategory == category) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                        placeholder = { Text("Search Affiliates") },
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 192.dp, start = 16.dp, end = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAffiliates) { affiliate ->
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
                                            db.collection("users")
                                                .document(walletId)
                                                .collection("affiliateClicks")
                                                .add(
                                                    mapOf(
                                                        "affiliateName" to affiliate.name,
                                                        "timestamp" to Instant.now().toString()
                                                    )
                                                ).await()
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
                                            context.startActivity(intent)
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
                                    if (affiliate.logoUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = affiliate.logoUrl,
                                            contentDescription = "Affiliate Logo for ${affiliate.name}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Text(
                                            text = affiliate.name.take(1),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = affiliate.cryptoBackOffer,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (isLoading) {
                        item(span = { GridItemSpan(3) }) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
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
                                onClick = { navigateToMarketplace() },
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
                                        contentDescription = "Marketplace"
                                    )
                                    Text(
                                        text = "Marketplace",
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
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AffiliatesScreenPreview() {
    F4cetMobileTheme {
        AffiliatesScreen(
            userProfile = User.Profile(name = "Test User", avatar = "", email = "", nfts = emptyList()),
            navigateBack = {},
            navigateToMarketplace = {}
        )
    }
}