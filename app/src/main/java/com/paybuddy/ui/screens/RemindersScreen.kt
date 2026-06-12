package com.paybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.paybuddy.ui.reminders.ReminderItem
import com.paybuddy.ui.reminders.ReminderUiState
import com.paybuddy.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    uiState: ReminderUiState,
    onSendWhatsApp: (ReminderItem) -> Unit,
    onClearError: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "Upcoming", "Overdue")
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val endOfToday = today + 86400000L

    val filteredList = when (selectedTab) {
        0 -> uiState.reminders.filter { 
            it.installment.dueDate in today until endOfToday && it.installment.status != "PAID" 
        }
        1 -> uiState.reminders.filter { 
            it.installment.dueDate >= endOfToday && it.installment.status != "PAID" 
        }
        2 -> uiState.reminders.filter { 
            (it.installment.status == "OVERDUE" || it.installment.dueDate < today) && it.installment.status != "PAID" 
        }
        else -> emptyList()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Payment Reminders", fontWeight = FontWeight.ExtraBold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassBg)
                    .border(1.dp, GlassEdge, RoundedCornerShape(16.dp))
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = NeonBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonBlue
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) NeonBlue else TextSecondary
                                ) 
                            }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else if (uiState.errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassBg)
                            .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(uiState.errorMessage, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onClearError,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dismiss", color = NeonRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "All caught up!", 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "No pending reminders for ${tabs[selectedTab].lowercase()}.", 
                            style = MaterialTheme.typography.bodyMedium, 
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredList) { reminderItem ->
                        ReminderCard(reminderItem, onSendWhatsApp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderCard(reminderItem: ReminderItem, onSendWhatsApp: (ReminderItem) -> Unit) {
    val installment = reminderItem.installment
    val isPhoneMissing = reminderItem.customerPhone.isBlank()
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val lastSentDate = if (installment.lastReminderSentAt > 0) {
        sdf.format(Date(installment.lastReminderSentAt))
    } else {
        "Never"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBg.copy(alpha = 0.7f))
            .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminderItem.customerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = reminderItem.customerPhone.ifBlank { "No Phone" },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonBlue
                            )
                        }
                    }
                }
                
                val statusColor = when(installment.status) {
                    "OVERDUE" -> NeonRed
                    "PAID" -> NeonGreen
                    else -> NeonAmber
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = installment.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "ITEM DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = reminderItem.itemName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PENDING AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "₹${"%.0f".format(Locale.ENGLISH, installment.amount - installment.amountPaid)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = NeonBlue
                    )
                    Text(
                        text = "Due on " + sdf.format(Date(installment.dueDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { onSendWhatsApp(reminderItem) },
                    enabled = !isPhoneMissing,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPhoneMissing) Color.White.copy(alpha = 0.05f) else Color(0xFF25D366).copy(alpha = 0.15f),
                        contentColor = if (isPhoneMissing) Color.Gray else Color(0xFF25D366)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    border = if (isPhoneMissing) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "WhatsApp Reminder", modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            HorizontalDivider(color = GlassEdge, thickness = 0.5.dp)
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(TextSecondary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reminders Sent: ${installment.reminderCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "Last: $lastSentDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
