package com.paybuddy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.*
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    customerId: String,
    saleId: String?,
    vendorId: String,
    viewModel: SalesViewModel,
    installments: List<Installment>,
    onPaymentRecorded: () -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var selectedInstallment by remember { mutableStateOf<Installment?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    var showPaymentModeMenu by remember { mutableStateOf(false) }
    var showInstallmentMenu by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    
    val saleContext by if (!saleId.isNullOrEmpty()) {
        viewModel.getSaleById(saleId).collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }

    LaunchedEffect(saleContext) {
        if (saleContext != null && amount.isEmpty()) {
            val rem = saleContext!!.remainingAmount
            amount = if (rem % 1 == 0.0) "%.0f".format(Locale.ENGLISH, rem) else "%.2f".format(Locale.ENGLISH, rem)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF111827), Color(0xFF0F172A))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        "Record Payment",
                        style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Amount Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF334155), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Amount to Pay", color = Color(0xFF94A3B8), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹", color = Color(0xFF4ADE80), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = amount,
                                onValueChange = { amount = it },
                                textStyle = TextStyle(
                                    color = Color(0xFF4ADE80),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(Color(0xFF4ADE80)),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Paying for Card
                if (saleContext != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF334155), Color.Transparent)))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Paying for: ${saleContext!!.itemName}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total Remaining: ₹${saleContext!!.remainingAmount}",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Payment Mode Dropdown
                Column {
                    Text("Payment Mode", color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaymentModeMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF334155), Color.Transparent)))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(paymentMode, color = Color.White, fontSize = 16.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                        DropdownMenu(
                            expanded = showPaymentModeMenu,
                            onDismissRequest = { showPaymentModeMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.85f)
                        ) {
                            listOf("CASH", "UPI", "CARD").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode, color = Color.White) },
                                    onClick = {
                                        paymentMode = mode
                                        showPaymentModeMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Installment Link Dropdown
                Column {
                    Text("Link to Installment", color = Color(0xFF94A3B8), fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showInstallmentMenu = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF334155), Color.Transparent)))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedInstallment == null) "Select Installment (Optional)" 
                                           else "Due: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedInstallment!!.dueDate))} - ₹${selectedInstallment!!.remainingAmount}",
                                    color = if (selectedInstallment == null) Color(0xFF94A3B8) else Color.White,
                                    fontSize = 16.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                        DropdownMenu(
                            expanded = showInstallmentMenu,
                            onDismissRequest = { showInstallmentMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B)).fillMaxWidth(0.85f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("None", color = Color.White) },
                                onClick = {
                                    selectedInstallment = null
                                    showInstallmentMenu = false
                                }
                            )
                            installments.forEach { inst ->
                                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(inst.dueDate))
                                DropdownMenuItem(
                                    text = { Text("Due: $dateStr - ₹${inst.remainingAmount}", color = Color.White) },
                                    onClick = {
                                        selectedInstallment = inst
                                        amount = inst.remainingAmount.toString()
                                        showInstallmentMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0) {
                            Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        val payment = Payment(
                            paymentId = "PAY_${System.currentTimeMillis()}",
                            saleId = saleId ?: "",
                            installmentId = selectedInstallment?.installmentId,
                            customerId = customerId,
                            vendorId = vendorId,
                            amount = amt,
                            paymentMode = paymentMode,
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.recordPayment(payment) {
                            isSaving = false
                            onPaymentRecorded()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80)),
                    enabled = !isSaving && amount.isNotEmpty()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Confirm Payment", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
