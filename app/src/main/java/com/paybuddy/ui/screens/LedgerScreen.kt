package com.paybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.LedgerEntry
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    vendorId: String,
    customerId: String,
    customerName: String,
    viewModel: CustomerViewModel,
    onBack: () -> Unit
) {
    val ledgerEntries by viewModel.getLedgerByCustomer(vendorId, customerId).collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = {
                    Column {
                        Text("Ledger History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(customerName, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (ledgerEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No ledger entries found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ledgerEntries) { entry ->
                    LedgerItemCard(entry)
                }
            }
        }
    }
}

@Composable
fun LedgerItemCard(entry: LedgerEntry) {
    val isSale = entry.type.lowercase() == "sale"
    val accentColor = if (isSale) NeonBlue else NeonGreen
    val icon = if (isSale) Icons.AutoMirrored.Rounded.ReceiptLong else Icons.Rounded.AccountBalanceWallet
    
    val dateText = try {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
    } catch (e: Exception) {
        "Unknown Date"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg.copy(alpha = 0.4f))
            .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.itemName.ifBlank { if (isSale) "Sale Entry" else "Payment Entry" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isSale) "-" else "+"} ₹${String.format(Locale.getDefault(), "%.0f", entry.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSale) Color.White else NeonGreen
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = entry.type.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}
