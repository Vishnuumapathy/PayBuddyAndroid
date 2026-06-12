package com.paybuddy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.Installment
import com.paybuddy.data.model.Payment
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.SalesViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    viewModel: SalesViewModel,
    customerId: String,
    saleId: String? = null,
    vendorId: String,
    installments: List<Installment>,
    onPaymentRecorded: () -> Unit,
    onBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf("CASH") }
    var selectedInstallment by remember { mutableStateOf<Installment?>(null) }
    var expandedMode by remember { mutableStateOf(false) }
    var expandedInst by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            expandedMode = false
            expandedInst = false
        }
    }

    val paymentModes = listOf("CASH", "UPI", "BANK")
    val context = LocalContext.current
    val view = LocalView.current
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            isSaving = false
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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = { Text("Record Payment", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Amount Field with Glass Style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassBg.copy(alpha = 0.7f))
                    .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Amount to Pay",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonGreen
                        ),
                        placeholder = { 
                            Text(
                                "0.00", 
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonGreen.copy(alpha = 0.3f)
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        enabled = !isSaving,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            cursorColor = NeonGreen,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        prefix = { 
                            Text(
                                "₹ ", 
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonGreen
                            ) 
                        }
                    )
                }
            }

            if (saleContext != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeonBlue.copy(alpha = 0.1f))
                        .border(0.5.dp, NeonBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Paying for: ${saleContext!!.itemName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Remaining: ₹ ${"%.0f".format(Locale.ENGLISH, saleContext!!.remainingAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Payment Mode Dropdown with Glass Style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBg.copy(alpha = 0.4f))
                    .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                ExposedDropdownMenuBox(
                    expanded = expandedMode,
                    onExpandedChange = { if (!isSaving) expandedMode = !expandedMode },
                ) {
                    TextField(
                        value = selectedPaymentMode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Mode", color = TextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMode) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isSaving,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMode,
                        onDismissRequest = { expandedMode = false },
                        modifier = Modifier.background(BackgroundDark)
                    ) {
                        paymentModes.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode, color = Color.White) },
                                onClick = {
                                    selectedPaymentMode = mode
                                    expandedMode = false
                                }
                            )
                        }
                    }
                }
            }

            // Optional Installment Link with Glass Style
            if (installments.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassBg.copy(alpha = 0.4f))
                        .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedInst,
                        onExpandedChange = { if (!isSaving) expandedInst = !expandedInst },
                    ) {
                        TextField(
                            value = selectedInstallment?.let { "Due: ₹${"%.0f".format(Locale.ENGLISH, it.amount)}" } ?: "Select Installment (Optional)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Link to Installment", color = TextSecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInst) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = !isSaving,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedInst,
                            onDismissRequest = { expandedInst = false },
                            modifier = Modifier.background(BackgroundDark)
                        ) {
                            installments.forEach { inst ->
                                DropdownMenuItem(
                                    text = { Text("Due: ₹${"%.0f".format(Locale.ENGLISH, inst.amount)} (Pending: ₹${"%.0f".format(Locale.ENGLISH, inst.amount - inst.amountPaid)})", color = Color.White) },
                                    onClick = {
                                        selectedInstallment = inst
                                        val pending = inst.amount - inst.amountPaid
                                        amount = if (pending % 1 == 0.0) "%.0f".format(Locale.ENGLISH, pending) else "%.2f".format(Locale.ENGLISH, pending)
                                        expandedInst = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (amountDouble == null || amountDouble <= 0) {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (vendorId.isEmpty()) {
                        Toast.makeText(context, "Vendor ID missing", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSaving = true
                    expandedMode = false
                    expandedInst = false

                    val payment = Payment(
                        paymentId = UUID.randomUUID().toString(),
                        saleId = saleId ?: selectedInstallment?.saleId ?: "",
                        installmentId = selectedInstallment?.installmentId,
                        customerId = customerId,
                        vendorId = vendorId,
                        amount = amountDouble,
                        paymentMode = selectedPaymentMode,
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.recordPayment(payment) {
                        isSaving = false
                        view.post { onPaymentRecorded() }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                enabled = amount.isNotBlank() && !isSaving
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(NeonGreen, Color(0xFF059669)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Confirm Payment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
