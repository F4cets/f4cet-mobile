package com.f4cets.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale // CHANGED: Added missing import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import coil.compose.AsyncImage
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            F4cetMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "homePrescreen") {
        composable("homePrescreen") {
            HomePrescreen(navigateToMain = {
                navController.navigate("mainScreen") {
                    popUpTo("homePrescreen") { inclusive = true }
                }
            })
        }
        composable("mainScreen") {
            MainScreen()
        }
    }
}

data class AffiliateClick(
    val affiliateName: String = "",
    val timestamp: String = ""
)

data class MarketplaceItem(
    val productId: String = "",
    val imageUrl: String = ""
)

data class AffiliateItem(
    val affiliateId: String = "",
    val name: String = "",
    val logoUrl: String = ""
)

@Composable
fun MainScreen() {
    var affiliateClicks by remember { mutableStateOf<List<AffiliateClick>>(emptyList()) }
    var logoUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var userProfile by remember { mutableStateOf<User.Profile?>(null) }
    var marketplaceItems by remember { mutableStateOf<List<MarketplaceItem>>(emptyList()) }
    var affiliateItems by remember { mutableStateOf<List<AffiliateItem>>(emptyList()) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"
        
        // Fetch affiliate clicks
        val clickDocuments = db.collection("users")
            .document(walletId)
            .collection("affiliateClicks")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
        val clicks = clickDocuments.mapNotNull { doc ->
            val affiliateName = doc.getString("affiliateName") ?: return@mapNotNull null
            val timestamp = doc.getString("timestamp") ?: return@mapNotNull null
            AffiliateClick(affiliateName, timestamp)
        }
        affiliateClicks = clicks

        // Fetch affiliate logo URLs for clicks
        val urlMap = mutableMapOf<String, String>()
        if (clicks.isNotEmpty()) {
            val affiliateNames = clicks.map { it.affiliateName }.distinct()
            affiliateNames.chunked(10).forEach { chunk ->
                val affiliateDocs = db.collection("affiliates")
                    .whereIn("name", chunk)
                    .get()
                    .await()
                affiliateDocs.documents.forEach { doc ->
                    val name = doc.getString("name") ?: ""
                    val logoUrl = doc.getString("logoUrl") ?: ""
                    if (name.isNotEmpty()) {
                        urlMap[name] = logoUrl
                    }
                }
            }
        }
        logoUrls = urlMap

        // Fetch user profile
        val profileDoc = db.collection("users")
            .document(walletId)
            .get()
            .await()
        val profileMap = profileDoc.get("profile") as? Map<*, *>
        userProfile = if (profileMap != null) {
            User.Profile(
                name = profileMap["name"] as? String ?: "User",
                avatar = profileMap["avatar"] as? String ?: "",
                email = profileMap["email"] as? String ?: "",
                nfts = (profileMap["nfts"] as? List<Map<String, Any>>) ?: emptyList()
            )
        } else {
            null
        }

        // Fetch first 10 marketplace items
        val productDocuments = db.collection("products")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
        val items = productDocuments.mapNotNull { doc ->
            val productId = doc.id
            val imageUrl = doc.getString("selectedImage") ?: return@mapNotNull null
            MarketplaceItem(productId, imageUrl)
        }
        marketplaceItems = items

        // Fetch first 10 affiliates
        val affiliateDocuments = db.collection("affiliates")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
        val affiliates = affiliateDocuments.mapNotNull { doc ->
            val affiliateId = doc.id
            val name = doc.getString("name") ?: return@mapNotNull null
            val logoUrl = doc.getString("logoUrl") ?: ""
            AffiliateItem(affiliateId, name, logoUrl)
        }
        affiliateItems = affiliates
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
                                text = "A",
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
                        value = "",
                        onValueChange = { /* Handle search input */ },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Search") },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 192.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            val clickCount = affiliateClicks.size
                            if (clickCount == 0) {
                                item {
                                    ElevatedCard(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .size(width = 200.dp, height = 140.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Add,
                                                contentDescription = "Add Affiliate",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            } else {
                                val itemsToShow = if (clickCount > 10) affiliateClicks.take(10) else affiliateClicks.take(4)
                                items(itemsToShow) { click ->
                                    val logoUrl = logoUrls[click.affiliateName] ?: ""
                                    ElevatedCard(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .size(width = 230.dp, height = 140.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (logoUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = logoUrl,
                                                    contentDescription = "Affiliate Logo for ${click.affiliateName}",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                    error = painterResource(id = R.drawable.bgf)
                                                )
                                            } else {
                                                Text(
                                                    text = click.affiliateName.take(1),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF727272),
                            textDecoration = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 6.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            items(marketplaceItems) { product ->
                                ElevatedCard(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .size(width = 200.dp, height = 180.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = "Marketplace Item ${product.productId}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            error = painterResource(id = R.drawable.bgf)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Marketplace",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF727272),
                            textDecoration = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 6.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            items(affiliateItems) { affiliate ->
                                ElevatedCard(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .size(width = 200.dp, height = 180.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (affiliate.logoUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = affiliate.logoUrl,
                                                contentDescription = "Affiliate Logo for ${affiliate.name}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                error = painterResource(id = R.drawable.bgf)
                                            )
                                        } else {
                                            Text(
                                                text = affiliate.name.take(1),
                                                style = MaterialTheme.typography.bodyMedium,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Affiliates",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF727272),
                            textDecoration = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 6.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        // Secondary FABs (shown when expanded)
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
                                onClick = { /* Placeholder for Home action */ },
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
                        // Primary FAB
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
fun MainScreenPreview() {
    F4cetMobileTheme {
        MainScreen()
    }
}