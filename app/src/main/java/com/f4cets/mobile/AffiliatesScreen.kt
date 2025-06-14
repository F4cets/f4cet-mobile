package com.f4cets.mobile

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant

@Composable
fun AffiliatesScreen(
    userProfile: User.Profile?,
    navigateBack: () -> Unit
) {
    var affiliateItems by remember { mutableStateOf<List<AffiliateItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var lastDocument by remember { mutableStateOf<DocumentSnapshot?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"
    val context = LocalContext.current

    // Load initial or searched affiliates
    LaunchedEffect(searchQuery) {
        isLoading = true
        affiliateItems = emptyList()
        lastDocument = null
        val db = FirebaseFirestore.getInstance()
        val query = db.collection("affiliates")
            .orderBy("createdAt")
            .limit(25) // CHANGED: Reduced batch size to 25
        Log.d("AffiliatesScreen", "Fetching initial batch: collection=affiliates, limit=25")
        val affiliateDocuments = try {
            query.get().await()
        } catch (e: Exception) {
            Log.e("AffiliatesScreen", "Initial fetch error: ${e.message}")
            isLoading = false
            return@LaunchedEffect
        }
        val affiliates = affiliateDocuments.mapNotNull { doc ->
            val affiliateId = doc.id
            val name = doc.getString("name") ?: return@mapNotNull null
            val logoUrl = doc.getString("logoUrl") ?: ""
            val cryptoBackOffer = doc.getString("cryptoBackOffer") ?: "Upto 5% Crypto Cashback"
            val createdAt = doc.get("createdAt")?.toString() ?: "null"
            Log.d("AffiliatesScreen", "Document: id=$affiliateId, name=$name, createdAt=$createdAt")
            if (searchQuery.trim().isEmpty() || name.lowercase().contains(searchQuery.trim().lowercase())) {
                AffiliateItem(affiliateId, name, logoUrl, cryptoBackOffer)
            } else {
                null
            }
        }
        affiliateItems = affiliates
        lastDocument = if (affiliateDocuments.documents.isNotEmpty()) affiliateDocuments.documents.lastOrNull() else null
        Log.d("AffiliatesScreen", "Initial fetch: loaded=${affiliates.size}, total=${affiliateItems.size}, lastDocument=${lastDocument?.id}")
        // CHANGED: Fallback query if fewer than expected
        if (affiliates.size < 25 && lastDocument != null) {
            Log.d("AffiliatesScreen", "Running fallback query to fetch all affiliates")
            val fallbackQuery = db.collection("affiliates").get()
            val fallbackDocs = try {
                fallbackQuery.await()
            } catch (e: Exception) {
                Log.e("AffiliatesScreen", "Fallback fetch error: ${e.message}")
                isLoading = false
                return@LaunchedEffect
            }
            val fallbackAffiliates = fallbackDocs.mapNotNull { doc ->
                val affiliateId = doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val logoUrl = doc.getString("logoUrl") ?: ""
                val cryptoBackOffer = doc.getString("cryptoBackOffer") ?: "Upto 5% Crypto Cashback"
                val createdAt = doc.get("createdAt")?.toString() ?: "null"
                Log.d("AffiliatesScreen", "Fallback document: id=$affiliateId, name=$name, createdAt=$createdAt")
                if (searchQuery.trim().isEmpty() || name.lowercase().contains(searchQuery.trim().lowercase())) {
                    AffiliateItem(affiliateId, name, logoUrl, cryptoBackOffer)
                } else {
                    null
                }
            }
            affiliateItems = fallbackAffiliates
            lastDocument = null // No pagination after fallback
            Log.d("AffiliatesScreen", "Fallback fetch: loaded=${fallbackAffiliates.size}, total=${affiliateItems.size}")
        }
        isLoading = false
    }

    // Load more affiliates on scroll
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val visibleItems = layoutInfo.visibleItemsInfo
                val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
                if (!isLoading && lastDocument != null && lastVisibleIndex >= totalItems - 5 && totalItems > 0) {
                    Log.d("AffiliatesScreen", "Triggering pagination: lastVisibleIndex=$lastVisibleIndex, totalItems=$totalItems, lastDocument=${lastDocument?.id}")
                    isLoading = true
                    val db = FirebaseFirestore.getInstance()
                    val query = db.collection("affiliates")
                        .orderBy("createdAt")
                        .startAfter(lastDocument)
                        .limit(25) // CHANGED: Reduced batch size to 25
                    Log.d("AffiliatesScreen", "Fetching next batch: collection=affiliates, startAfter=${lastDocument?.id}, limit=25")
                    val affiliateDocuments = try {
                        query.get().await()
                    } catch (e: Exception) {
                        Log.e("AffiliatesScreen", "Pagination fetch error: ${e.message}")
                        isLoading = false
                        return@collect
                    }
                    val newAffiliates = affiliateDocuments.mapNotNull { doc ->
                        val affiliateId = doc.id
                        val name = doc.getString("name") ?: return@mapNotNull null
                        val logoUrl = doc.getString("logoUrl") ?: ""
                        val cryptoBackOffer = doc.getString("cryptoBackOffer") ?: "Upto 5% Crypto Cashback"
                        val createdAt = doc.get("createdAt")?.toString() ?: "null"
                        Log.d("AffiliatesScreen", "Document: id=$affiliateId, name=$name, createdAt=$createdAt")
                        if (searchQuery.trim().isEmpty() || name.lowercase().contains(searchQuery.trim().lowercase())) {
                            AffiliateItem(affiliateId, name, logoUrl, cryptoBackOffer)
                        } else {
                            null
                        }
                    }
                    affiliateItems = affiliateItems + newAffiliates
                    lastDocument = if (affiliateDocuments.documents.isNotEmpty()) affiliateDocuments.documents.lastOrNull() else null
                    Log.d("AffiliatesScreen", "Next fetch: loaded=${newAffiliates.size}, total=${affiliateItems.size}, lastDocument=${lastDocument?.id}")
                    isLoading = false
                }
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
                        .padding(top = 16.dp, start = 16.dp)
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
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Shopping Cart"
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp, start = 16.dp, end = 16.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
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
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        IconButton(
                            onClick = { /* Handle filter action */ }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.List,
                                contentDescription = "Filter List",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 192.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Affiliate Stores",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF727272),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                    items(affiliateItems) { affiliate ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .widthIn(min = 200.dp, max = 300.dp)
                        ) {
                            ElevatedCard(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 2f)
                                    .clickable {
                                        coroutineScope.launch {
                                            // Track click
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
                                            // Open link (placeholder)
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
                                            context.startActivity(intent)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (affiliate.logoUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = affiliate.logoUrl,
                                            contentDescription = "Affiliate Logo for ${affiliate.name}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(3f / 2f),
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
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(horizontal = 16.dp, vertical = 16.dp)
                                            .fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (isLoading) {
                        item {
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
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scale by animateFloatAsState(if (isFabMenuExpanded) 1f else 0f)
                        if (isFabMenuExpanded) {
                            FloatingActionButton(
                                onClick = { /* Placeholder for Receipt action */ },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Face,
                                    contentDescription = "Receipt"
                                )
                            }
                            FloatingActionButton(
                                onClick = { /* Placeholder for Search action */ },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search"
                                )
                            }
                            FloatingActionButton(
                                onClick = { navigateBack() },
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
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
            navigateBack = {}
        )
    }
}