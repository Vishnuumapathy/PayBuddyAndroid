package com.paybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.Sale
import com.paybuddy.ui.theme.*
import com.paybuddy.utils.WhatsAppHelper
import com.paybuddy.viewmodel.SalesViewModel
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: String,
    viewModel: SalesViewModel,
    onRecordPayment: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val sale by viewModel.getSaleById(saleId).collectAsState(initial = null)
    val customer by remember(sale?.customerId) {
        if (sale != null) {
            viewModel.getCustomerById(sale!!.customerId)
        } else {
            flowOf(null)
        }
    }.collectAsState(initial = null)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Sale Details", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (sale == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonBlue)
            }
        } else {
            val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
            
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Main Info Card
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
                                    text = sale!!.itemName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sale!!.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = NeonBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                customer?.phone?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            StatusBadge(sale!!.status)
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassEdge.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        DetailRow("Total Price", "₹ ${"%.2f".format(Locale.ENGLISH, sale!!.finalAmount)}", isBold = true, valueColor = Color.White)
                        DetailRow("Total Paid", "₹ ${"%.2f".format(Locale.ENGLISH, sale!!.amountPaid)}", valueColor = NeonGreen)
                        DetailRow("Total Remaining", "₹ ${"%.2f".format(Locale.ENGLISH, sale!!.remainingAmount)}", valueColor = NeonRed, isBold = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Details Section
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassBg.copy(alpha = 0.4f))
                        .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        DetailRow("Sale Date", sdf.format(Date(sale!!.createdAt)))
                        DetailRow("Item Price", "₹ ${"%.2f".format(Locale.ENGLISH, sale!!.unitPrice)}")
                        DetailRow("Quantity", "${sale!!.quantity}")
                        
                        if (sale!!.paymentType == "Partial Payment") {
                            DetailRow("Installment Count", "${sale!!.installmentCount}")
                            DetailRow("Interest", "${sale!!.interestRate}%")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                val context = LocalContext.current
                
                // WhatsApp Button (Gradient)
                Button(
                    onClick = {
                        val phone = customer?.phone
                        if (!phone.isNullOrBlank()) {
                            val remainingFormatted = if (sale!!.remainingAmount % 1 == 0.0) {
                                "%.0f".format(Locale.ENGLISH, sale!!.remainingAmount)
                            } else {
                                "%.2f".format(Locale.ENGLISH, sale!!.remainingAmount)
                            }
                            
                            val message = "Hi ${sale!!.customerName}, regarding your purchase of ${sale!!.itemName}, your remaining balance is ₹$remainingFormatted."
                            WhatsAppHelper.sendReminder(context, phone, message)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Gray.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !customer?.phone.isNullOrBlank()
                ) {
                    val brush = if (!customer?.phone.isNullOrBlank()) {
                        Brush.horizontalGradient(listOf(Color(0xFF25D366), Color(0xFF128C7E)))
                    } else {
                        Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.5f)))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Send WhatsApp Reminder", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Record Payment Button (Glassy)
                OutlinedButton(
                    onClick = { onRecordPayment(sale!!.customerId, sale!!.saleId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f)),
                    enabled = sale!!.remainingAmount > 0.01
                ) {
                    Text("Record Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, bgColor) = if (status == "COMPLETED") {
        NeonGreen to NeonGreen.copy(alpha = 0.1f)
    } else {
        NeonAmber to NeonAmber.copy(alpha = 0.1f)
    }
    
    Surface(
        color = bgColor,
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(
    label: String, 
    value: String, 
    isBold: Boolean = false,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value, 
            style = MaterialTheme.typography.bodyLarge, 
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium,
            color = valueColor
        )
    }
}


