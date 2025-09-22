package com.example.werun.ui.screens.landing

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LandingScreen(onTimeout: () -> Unit, viewModel: LandingViewModel) {
    // This LaunchedEffect will run once when the composable enters the screen.
    // After a delay, it will trigger the onTimeout navigation event.
    LaunchedEffect(key1 = true) {
        delay(viewModel.splashWaitTime)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Set the background color to white
        contentAlignment = Alignment.Center // Center content vertically and horizontally
    ) {
        WeRunLogo()
    }
}

@Composable
fun WeRunLogo() {
    // A Column to arrange the text and the underline vertically.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The "WeRun" text
        Text(
            text = "WeRun",
            color = Color.Black,
            fontSize = 60.sp, // Adjust size as needed
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.SansSerif // A generic sans-serif font
        )

        // The lime green underline
        Box(
            modifier = Modifier
                .width(220.dp) // Adjust width to match the text
                .height(4.dp)  // Adjust height for thickness
                .background(Color(0xFFADFF2F)) // A lime green color
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun LandingScreenPreview() {
    // In the preview, we can pass a dummy ViewModel and an empty lambda.
    LandingScreen(onTimeout = {}, viewModel = LandingViewModel())
}