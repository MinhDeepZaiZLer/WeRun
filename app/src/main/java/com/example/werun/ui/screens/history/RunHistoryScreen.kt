package com.example.werun.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel // Add this import for hiltViewModel
import com.example.werun.data.model.RunData
//import com.example.werun.viewmodel.HistoryViewModel // Ensure correct package
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningHistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel() // Changed to hiltViewModel()
) {
    val runDataList by viewModel.runDataList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle navigation icon click */ }) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF0F0F0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Total Kilometers Section
            if (runDataList.isNotEmpty()) {
                val totalDistance = runDataList.sumOf { it.distance / 1000.0 }
                Text(
                    text = String.format("%.2f", totalDistance),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Text(
                    text = "Kilometer",
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem(label = "Run", value = runDataList.size.toString())
                    SummaryItem(
                        label = "Average Pace",
                        value = if (runDataList.isNotEmpty()) {
                            val avgPace = runDataList.map { it.averageSpeed }
                                .filter { it > 0 }
                                .takeIf { it.isNotEmpty() }?.average()?.let { 60.0 / it }
                            if (avgPace != null) String.format("%.2f", avgPace) else "--"
                        } else "--"
                    )
                    SummaryItem(
                        label = "Time",
                        value = runDataList.sumOf { it.duration }.let { duration ->
                            val hours = duration / (1000 * 60 * 60)
                            val minutes = (duration / (1000 * 60)) % 60
                            val seconds = (duration / 1000) % 60
                            if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
                            else String.format("%02d:%02d", minutes, seconds)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No runs yet",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Your running history will appear here",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                return@Column
            }

            // History List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(runDataList) { runData ->
                    RunHistoryCard(runData = runData)
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun RunHistoryCard(runData: RunData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Star, Date, Run Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Star Icon
                if (runData.isFavorite) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = Color(0xFFC0F028),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))

                // Date and Run Type
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${runData.getDayOfWeek()} ${runData.getRunType()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = runData.getFormattedDate(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Distance
                Text(
                    text = runData.getFormattedDistance(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFC0F028)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Basic Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Time: ${runData.getFormattedTime()}",
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Text(
                    text = "Pace: ${runData.getFormattedPace()}/km",
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Additional Info Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Calories: ${runData.calories}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Elevation: +${runData.elevationGain} m",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Additional Info Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weather: ${runData.weather.ifEmpty { "—" }}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Notes: ${runData.notes.ifEmpty { "—" }}",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Route points info
            if (runData.routePoints.isNotEmpty()) {
                Text(
                    text = "Route: ${runData.routePoints.size} points",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// Preview helper (to avoid ViewModel in preview)
@Composable
private fun RunningHistoryScreenWithSample(runDataList: List<RunData>) {
    // Duplicate the Scaffold/Column logic here but use runDataList directly
    // ... (omit for brevity; copy from above and replace viewModel states with param)
}