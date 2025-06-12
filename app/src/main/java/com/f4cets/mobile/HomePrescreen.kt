package com.f4cets.mobile

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.f4cets.mobile.ui.theme.F4cetMobileTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin

@Composable
fun HomePrescreen(navigateToMain: () -> Unit) {
    // Use Scaffold to manage layout and FAB positioning, removed topBar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            var isPulsing by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPulsing) 1.1f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            LaunchedEffect(Unit) {
                while (true) {
                    isPulsing = true
                    delay(1000) // Sync with animation duration
                    isPulsing = false
                    delay(1000) // Pause before next pulse
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.Easing { t -> t * t * (3 - 2 * t) })),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                FloatingActionButton(
                    onClick = {
                        // Simulate wallet connection with hardcoded wallet
                        navigateToMain()
                    },
                    containerColor = Color(0xFFFF9999), // Peach color from bg.png
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = 16.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                ) {
                    Text(
                        text = "Please connect your wallet",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            }
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg),
                    contentDescription = "Background Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                val rotation = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        rotation.animateTo(
                            targetValue = 5f,
                            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                        )
                        rotation.animateTo(
                            targetValue = -5f,
                            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                        )
                        rotation.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                        )
                    }
                }
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "F4cets Logo",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(400.dp)
                        .graphicsLayer(
                            rotationZ = rotation.value,
                            transformOrigin = TransformOrigin(0.5f, 0f) // Pivot at top center
                        )
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomePrescreenPreview() {
    F4cetMobileTheme {
        HomePrescreen(navigateToMain = {})
    }
}