package com.paybuddy.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.*
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    customerId: String,
    vendorId: String,
    viewModel: CustomerViewModel,
    onBack: () -> Unit,
    onOtherSalesClick: (String) -> Unit,
    onViewLedgerClick: (String, String) -> Unit
) {
    val TAG = "CustomerProfileScreen"

    if (customerId.isBlank()) {
        Log.e(TAG, "Empty customerId provided")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Invalid Customer Information")
        }
        return
    }

    val customer by viewModel.getCustomerByIdFlow(customerId).collectAsState(initial = null)
    val sales by viewModel.getSalesByCustomer(vendorId, customerId).collectAsState(initial = emptyList())
    val installments by viewModel.getInstallmentsByCustomer(vendorId, customerId).collectAsState(initial = emptyList())
    
    val riskScore = remember(sales, installments) { 
        try {
            viewModel.calculateRiskScore(sales, installments)
        } catch (_: Exception) {
            Log.e(TAG, "Error calculating risk score")
            "Unknown"
        }
    }
    val latestSale = remember(sales) { sales.maxByOrNull { it.createdAt } }

    // Derive totals from sales data
    val totalSalesCount = sales.size
    val totalSalesValue = sales.sumOf { it.finalAmount }
    val totalPaidValue = sales.sumOf { it.amountPaid }
    val totalRemainingValue = sales.sumOf { it.remainingAmount }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = { Text(customer?.name ?: "Customer Profile", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CustomerOverviewCard(
                    name = customer?.name ?: "N/A",
                    phone = customer?.phone ?: "N/A",
                    riskScore = riskScore,
                    totalSalesCount = totalSalesCount,
                    totalSalesValue = totalSalesValue,
                    totalPaidValue = totalPaidValue,
                    totalRemainingValue = totalRemainingValue
                )
            }
                
            if (latestSale != null) {
                item {
                    Text(
                        text = "Latest Sale",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LatestSaleCard(latestSale)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onOtherSalesClick(customerId) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
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
                            Text("All Sales", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Button(
                        onClick = { onViewLedgerClick(customerId, customer?.name ?: "Customer") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GlassBg.copy(alpha = 0.5f))
                                .border(1.dp, NeonBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ledger", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CustomerOverviewCard(
    name: String,
    phone: String,
    riskScore: String,
    totalSalesCount: Int,
    totalSalesValue: Double,
    totalPaidValue: Double,
    totalRemainingValue: Double
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassBg.copy(alpha = 0.7f))
            .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name, 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = phone, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = TextSecondary
                    )
                }
                RiskScoreBadge(riskScore)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassEdge.copy(alpha = 0.5f)))
            Spacer(modifier = Modifier.height(20.dp))
            
            DetailRow("Total Sales", totalSalesCount.toString(), Color.White)
            DetailRow("Total Value", "₹ ${"%.0f".format(Locale.ENGLISH, totalSalesValue)}", NeonBlue)
            DetailRow("Total Paid", "₹ ${"%.0f".format(Locale.ENGLISH, totalPaidValue)}", NeonGreen)
            DetailRow("Outstanding", "₹ ${"%.0f".format(Locale.ENGLISH, totalRemainingValue)}", NeonRed)
        }
    }
}

@Composable
fun LatestSaleCard(sale: Sale) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg.copy(alpha = 0.4f))
            .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sale.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                val status = sale.status
                val (statusColor, statusBg) = if (status == "COMPLETED") {
                    NeonGreen to NeonGreen.copy(alpha = 0.1f)
                } else {
                    NeonAmber to NeonAmber.copy(alpha = 0.1f)
                }
                
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailRow("Amount", "₹ ${"%.0f".format(Locale.ENGLISH, sale.finalAmount)}", Color.White)
            DetailRow("Paid", "₹ ${"%.0f".format(Locale.ENGLISH, sale.amountPaid)}", NeonGreen)
            DetailRow("Remaining", "₹ ${"%.0f".format(Locale.ENGLISH, sale.remainingAmount)}", if (sale.remainingAmount > 0) NeonAmber else NeonGreen)
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Sale Date: ${sdf.format(Date(sale.createdAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RiskScoreBadge(riskScore: String) {
    val (color, label) = when (riskScore) {
        "Low Risk" -> NeonGreen to "LOW RISK"
        "Medium Risk" -> NeonAmber to "MEDIUM RISK"
        "High Risk" -> NeonRed to "HIGH RISK"
        else -> Color.Gray to riskScore.uppercase()
    }
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            style = MaterialTheme.typography.bodyMedium, 
            color = TextSecondary
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyLarge, 
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
