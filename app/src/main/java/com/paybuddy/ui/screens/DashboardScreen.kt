package com.paybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.LedgerEntry
import com.paybuddy.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vendorName: String,
    shopName: String,
    todayDue: Double,
    overdue: Double,
    upcoming: Double,
    totalOutstanding: Double,
    recentActivity: List<LedgerEntry>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onNewSaleClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onRemindersClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onErrorDismiss: () -> Unit = {}
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }

    Scaffold(
        containerColor = Color.Transparent, // Use the activity's gradient background
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Column {
                        Text(
                            text = "$greeting, $vendorName",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = shopName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRemindersClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminders")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GlassStatCard(label = "Today", value = "₹$todayDue", color = NeonAmber, modifier = Modifier.weight(1f))
                        GlassStatCard(label = "Overdue", value = "₹$overdue", color = NeonRed, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GlassStatCard(label = "Upcoming", value = "₹$upcoming", color = NeonBlue, modifier = Modifier.weight(1f))
                        GlassStatCard(label = "Total Out", value = "₹$totalOutstanding", color = NeonGreen, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = onNewSaleClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(listOf(NeonBlue, Color(0xFF1D4ED8)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("New Sale", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassBg.copy(alpha = 0.6f))
                                .border(1.dp, GlassEdge, RoundedCornerShape(16.dp))
                                .clickable { onRecordPaymentClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = NeonGreen)
                                Spacer(Modifier.width(8.dp))
                                Text("Payment", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = NeonBlue)
                        }
                    }
                }

                if (recentActivity.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassBg.copy(alpha = 0.3f))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent activity", color = TextSecondary)
                        }
                    }
                } else {
                    items(recentActivity) { entry ->
                        GlassActivityItem(entry)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun GlassStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBg.copy(alpha = 0.7f))
            .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(4.dp, 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun GlassActivityItem(entry: LedgerEntry) {
    val itemName = entry.itemName.ifBlank { "Unknown item" }
    val customerName = entry.customerName.ifBlank { "Unknown customer" }
    val dateText = try {
        if (entry.createdAt > 0) {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
        } else {
            "Date unavailable"
        }
    } catch (e: Exception) {
        "Date unavailable"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBg.copy(alpha = 0.4f))
            .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NeonBlue.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.0f", entry.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
