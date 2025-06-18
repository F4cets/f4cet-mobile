package com.f4cets.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
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

fun formatDate(timestamp: String?): String {
    return timestamp?.let {
        try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(it)?.let { date ->
                SimpleDateFormat("MMM d", Locale.US).format(date)
            } ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    } ?: "Unknown"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var userProfile by remember { mutableStateOf<User.Profile?>(null) }
    val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"

    NavHost(navController = navController, startDestination = "homePrescreen") {
        composable("homePrescreen") {
            HomePrescreen(navigateToMain = {
                navController.navigate("mainScreen") {
                    popUpTo("homePrescreen") { inclusive = true }
                }
            })
        }
        composable("mainScreen") {
            MainScreen(
                userProfile = userProfile,
                onUserProfileUpdated = { userProfile = it },
                navigateToAffiliates = { navController.navigate("affiliatesScreen") },
                navigateToMarketplace = { navController.navigate("marketplaceScreen") },
                navigateToProfile = { navController.navigate("profileScreen") },
                navigateToCart = { navController.navigate("cartScreen") }
            )
        }
        composable("affiliatesScreen") {
            AffiliatesScreen(
                userProfile = userProfile,
                navigateBack = { navController.popBackStack() },
                navigateToMarketplace = { navController.navigate("marketplaceScreen") }
            )
        }
        composable("marketplaceScreen") {
            MarketplaceScreen(
                userProfile = userProfile,
                navigateBack = { navController.popBackStack() },
                navigateToAffiliates = { navController.navigate("affiliatesScreen") },
                navigateToStore = { storeId -> navController.navigate("storeScreen/$storeId") },
                navigateToProduct = { productId -> navController.navigate("productScreen/$productId") }
            )
        }
        composable(
            "storeScreen/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            StoreScreen(
                userProfile = userProfile,
                storeId = backStackEntry.arguments?.getString("storeId") ?: "",
                navigateBack = { navController.popBackStack() },
                navigateToAffiliates = { navController.navigate("affiliatesScreen") },
                navigateToStore = { storeId -> navController.navigate("storeScreen/$storeId") },
                navigateToProduct = { productId -> navController.navigate("productScreen/$productId") }
            )
        }
        composable(
            "productScreen/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            ProductScreen(
                userProfile = userProfile,
                productId = backStackEntry.arguments?.getString("productId") ?: "",
                navigateBack = { navController.popBackStack() },
                navigateToAffiliates = { navController.navigate("affiliatesScreen") },
                navigateToStore = { storeId -> navController.navigate("storeScreen/$storeId") },
                navigateToProduct = { productId -> navController.navigate("productScreen/$productId") },
                navigateToCart = { navController.navigate("cartScreen") }
            )
        }
        composable("cartScreen") {
            CartScreen(
                userProfile = userProfile,
                navigateBack = { navController.popBackStack() },
                navigateToAffiliates = { navController.navigate("affiliatesScreen") },
                navigateToStore = { storeId -> navController.navigate("storeScreen/$storeId") },
                navigateToProduct = { productId -> navController.navigate("productScreen/$productId") }
            )
        }
        composable("profileScreen") {
            ProfileScreen(walletId = walletId)
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
    val logoUrl: String = "",
    val cryptoBackOffer: String = ""
)

data class OrderCardData(
    val productName: String,
    val orderDate: String,
    val amount: String,
    val status: String,
    val imageUrl: String,
    val transId: String
)

@Composable
fun MainScreen(
    userProfile: User.Profile?,
    onUserProfileUpdated: (User.Profile?) -> Unit,
    navigateToAffiliates: () -> Unit,
    navigateToMarketplace: () -> Unit,
    navigateToProfile: () -> Unit,
    navigateToCart: () -> Unit
) {
    var affiliateClicks by remember { mutableStateOf<List<AffiliateClick>>(emptyList()) }
    var logoUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var marketplaceItems by remember { mutableStateOf<List<MarketplaceItem>>(emptyList()) }
    var affiliateItems by remember { mutableStateOf<List<AffiliateItem>>(emptyList()) }
    var orderCards by remember { mutableStateOf<List<OrderCardData>>(emptyList()) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val walletId = "2FbdM2GpXGPgkt8tFEWSyjfiZH2Un2qx7rcm78coSbh7"

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

        val profileDoc = db.collection("users")
            .document(walletId)
            .get()
            .await()
        val profileMap = profileDoc.get("profile") as? Map<*, *>
        val newProfile = if (profileMap != null) {
            User.Profile(
                name = profileMap["name"] as? String ?: "User",
                avatar = profileMap["avatar"] as? String ?: "",
                email = profileMap["email"] as? String ?: "",
                nfts = (profileMap["nfts"] as? List<Map<String, Any>>) ?: emptyList(),
                shippingAddress = profileMap["shippingAddress"] as? String ?: ""
            )
        } else {
            null
        }
        onUserProfileUpdated(newProfile)

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

        val affiliateDocuments = db.collection("affiliates")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
        val affiliates = affiliateDocuments.mapNotNull { doc ->
            val affiliateId = doc.id
            val name = doc.getString("name") ?: return@mapNotNull null
            val logoUrl = doc.getString("logoUrl") ?: ""
            val cryptoBackOffer = doc.getString("cryptoBackOffer") ?: ""
            AffiliateItem(affiliateId, name, logoUrl, cryptoBackOffer)
        }
        affiliateItems = affiliates

        val transactionDocs = db.collection("transactions")
            .whereEqualTo("buyerId", walletId)
            .whereIn("status", listOf("Ordered", "Shipped", "Delivered"))
            .get()
            .await()
        val orders = transactionDocs.mapNotNull { doc ->
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
        orderCards = orders
    }

    LaunchedEffect(orderCards) {
        listState.animateScrollToItem(0)
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
                        .clickable { navigateToProfile() }
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
                    onClick = { navigateToCart() },
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
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 120.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        if (orderCards.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(orderCards) { orderCard ->
                                    OrderCard(
                                        productName = orderCard.productName,
                                        orderDate = orderCard.orderDate,
                                        amount = orderCard.amount,
                                        status = orderCard.status,
                                        imageUrl = orderCard.imageUrl
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Orders",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF727272),
                                textDecoration = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val clickCount = affiliateClicks.size
                            if (clickCount == 0) {
                                item {
                                    ElevatedCard(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .size(width = 240.dp, height = 140.dp)
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
                                            .size(width = 240.dp, height = 140.dp)
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF727272),
                            textDecoration = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 6.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF727272),
                            textDecoration = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 6.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(affiliateItems) { affiliate ->
                                ElevatedCard(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .size(width = 300.dp, height = 180.dp)
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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF727272),
                            textDecoration = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 6.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
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
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = "Affiliates"
                                    )
                                    Text(
                                        text = "Affiliates",
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
                                onClick = { navigateToProfile() },
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

@Composable
fun OrderCard(
    productName: String,
    orderDate: String,
    amount: String,
    status: String,
    imageUrl: String
) {
    ElevatedCard(
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .size(width = 320.dp, height = 120.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp)
            ) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ordered $orderDate",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (status) {
                        "Ordered" -> MaterialTheme.colorScheme.primary
                        "Shipped" -> MaterialTheme.colorScheme.tertiary
                        "Delivered" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            AsyncImage(
                model = imageUrl,
                contentDescription = "Order Item Image",
                modifier = Modifier
                    .size(100.dp)
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.bgf)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    F4cetMobileTheme {
        MainScreen(
            userProfile = null,
            onUserProfileUpdated = {},
            navigateToAffiliates = {},
            navigateToMarketplace = {},
            navigateToProfile = {},
            navigateToCart = {}
        )
    }
}