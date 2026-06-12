package com.paybuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paybuddy.data.model.Sale
import com.paybuddy.viewmodel.CustomerViewModel
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

fun safeFormatAmount(value: Double?): String {
    return try {
        if (value == null || value.isNaN() || value.isInfinite()) {
            "₹ 0"
        } else {
            "₹ %.2f".format(Locale.ENGLISH, value)
        }
    } catch (e: Exception) {
        "₹ 0"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSalesListScreen(
    customerId: String,
    vendorId: String,
    viewModel: CustomerViewModel,
    onBack: () -> Unit
) {
    val sales by viewModel.getSalesByCustomer(vendorId, customerId).collectAsState(initial = emptyList())
    val customer by viewModel.getCustomerByIdFlow(customerId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${customer?.name ?: "Customer"}'s Sales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (sales.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No sales found for this customer.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(sales) { sale ->
                    CustomerSaleCard(sale)
                }
            }
        }
    }
}

@Composable
fun CustomerSaleCard(sale: Sale) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    // Log invalid data for debugging
    if (sale.finalAmount < 0 || sale.amountPaid < 0 || sale.finalAmount.isNaN()) {
        Log.w("SALE_DATA", "Invalid sale data: $sale")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = sale.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val statusColor = if (sale.status == "COMPLETED") Color(0xFF388E3C) else Color(0xFFF57C00)
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = sale.status,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = sdf.format(Date(sale.createdAt)), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "Interest: ${sale.interestRate}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            
            DetailRowSmall("Total Amount", safeFormatAmount(sale.finalAmount))
            DetailRowSmall("Amount Paid", safeFormatAmount(sale.amountPaid))
            DetailRowSmall("Remaining", safeFormatAmount(sale.remainingAmount))
            DetailRowSmall("Installments", "${sale.installmentCount}")
        }
    }
}

@Composable
fun DetailRowSmall(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
