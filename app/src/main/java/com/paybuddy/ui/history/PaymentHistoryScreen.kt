package com.paybuddy.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.LedgerEntry
import com.paybuddy.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    payments: List<LedgerEntry>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                ),
                title = { Text("Payment History", fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text("Search payments...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonBlue) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = GlassBg.copy(alpha = 0.5f),
                    unfocusedContainerColor = GlassBg.copy(alpha = 0.3f),
                    focusedBorderColor = NeonBlue,
                    unfocusedBorderColor = GlassEdge,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else if (payments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No payment history yet" else "No matching payments found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(payments) { payment ->
                        PaymentCard(payment)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentCard(payment: LedgerEntry) {
    val customerName = payment.customerName.ifBlank { "Unknown Customer" }
    val saleName = payment.itemName.ifBlank { "Unnamed Sale" }
    val dateText = try {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(payment.createdAt))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = saleName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%.0f", payment.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonGreen.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "RECEIVED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
            }
        }
    }
}
