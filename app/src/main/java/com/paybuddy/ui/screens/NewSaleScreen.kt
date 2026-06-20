package com.paybuddy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.Customer
import com.paybuddy.data.model.Installment
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(
    viewModel: SalesViewModel,
    vendorId: String,
    customers: List<Customer>,
    onSaleCreated: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onBack: () -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("0") }
    var installmentCount by remember { mutableStateOf("1") }
    var amountPaid by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var paymentType by remember { mutableStateOf("Full Payment") }
    var isSaving by remember { mutableStateOf(false) }

    // Date and Interval States
    val calendar = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 30) } }
    var firstDueDate by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var installmentInterval by remember { mutableStateOf(2592000000L) } // Default 30 days (Monthly)
    var intervalExpanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Validation for Date
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val isPastDate = paymentType == "Partial Payment" && firstDueDate < todayStart

    // Fix for PopupLayout crash: Ensure dropdown is closed when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            expanded = false
        }
    }

    val totalAmount = (quantity.toDoubleOrNull() ?: 0.0) * (unitPrice.toDoubleOrNull() ?: 0.0)
    val interestPercent = if (paymentType == "Full Payment") 0.0 else (interestRate.toDoubleOrNull() ?: 0.0)
    val interestAmount = totalAmount * interestPercent / 100
    val finalAmount = totalAmount + interestAmount
    
    val amountPaidValue = amountPaid.toDoubleOrNull() ?: 0.0
    val isOverpaid = amountPaidValue > finalAmount + 0.01
    val isNegativePaid = amountPaidValue < 0.0
    val isInvalidCount = paymentType == "Partial Payment" && (installmentCount.toIntOrNull() ?: 0) <= 0
    val remainingAmount = finalAmount - amountPaidValue
    val count = (installmentCount.toIntOrNull() ?: 1).coerceAtLeast(1)
    val installmentAmount = remainingAmount / count

    val view = LocalView.current
    val context = LocalContext.current
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            isSaving = false
            viewModel.clearError()
        }
    }

    // Sync amountPaid with finalAmount if Full Payment is selected
    LaunchedEffect(paymentType, finalAmount) {
        if (paymentType == "Full Payment") {
            amountPaid = "%.2f".format(Locale.ENGLISH, finalAmount)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = firstDueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        firstDueDate = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("New Sale", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        expanded = false
                        view.post { onBack() }
                    }, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Form Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassBg.copy(alpha = 0.7f))
                        .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { if (!isSaving) expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Select Customer",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Customer") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = GlassEdge,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = NeonBlue,
                                    unfocusedLabelColor = TextSecondary
                                )
                            )
                            if (expanded) {
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("+ Add New Customer", color = NeonBlue, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            expanded = false
                                            view.post { onAddCustomerClick() }
                                        }
                                    )
                                    customers.forEach { customer ->
                                        DropdownMenuItem(
                                            text = { Text(customer.name) },
                                            onClick = {
                                                selectedCustomer = customer
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name") },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "item_name_input" },
                            enabled = !isSaving,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = GlassEdge,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f).semantics { contentDescription = "qty_input" },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = GlassEdge,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            OutlinedTextField(
                                value = unitPrice,
                                onValueChange = { unitPrice = it },
                                label = { Text("Unit Price") },
                                modifier = Modifier.weight(1f).semantics { contentDescription = "unit_price_input" },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = GlassEdge,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        OutlinedTextField(
                            value = "₹ %.2f".format(Locale.ENGLISH, totalAmount),
                            onValueChange = {},
                            label = { Text("Total Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = !isSaving,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonBlue,
                                unfocusedBorderColor = GlassEdge,
                                focusedTextColor = NeonBlue,
                                unfocusedTextColor = NeonBlue,
                                disabledTextColor = NeonBlue
                            )
                        )

                        Text("Payment Type", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = paymentType == "Full Payment",
                                onClick = { paymentType = "Full Payment" },
                                enabled = !isSaving,
                                colors = RadioButtonDefaults.colors(selectedColor = NeonBlue, unselectedColor = TextSecondary)
                            )
                            Text("Full", color = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = paymentType == "Partial Payment",
                                onClick = { paymentType = "Partial Payment" },
                                enabled = !isSaving,
                                colors = RadioButtonDefaults.colors(selectedColor = NeonBlue, unselectedColor = TextSecondary)
                            )
                            Text("Partial", color = Color.White)
                        }

                        if (paymentType == "Partial Payment") {
                            OutlinedTextField(
                                value = installmentCount,
                                onValueChange = { installmentCount = it },
                                label = { Text("Installment Count") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isSaving,
                                isError = isInvalidCount,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = GlassEdge,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                supportingText = if (isInvalidCount) {
                                    { Text("Count must be at least 1", color = NeonRed) }
                                } else null
                            )
                            OutlinedTextField(
                                value = interestRate,
                                onValueChange = { interestRate = it },
                                label = { Text("Interest Rate (%)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = !isSaving,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = GlassEdge,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            // First Due Date Picker
                            OutlinedTextField(
                                value = dateFormatter.format(Date(firstDueDate)),
                                onValueChange = {},
                                label = { Text("First Due Date") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { if (!isSaving) showDatePicker = true }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = NeonBlue)
                                    }
                                },
                                isError = isPastDate,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = GlassEdge,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                supportingText = if (isPastDate) {
                                    { Text("Date cannot be in the past", color = NeonRed) }
                                } else null,
                                enabled = !isSaving
                            )

                            // Installment Interval Dropdown
                            ExposedDropdownMenuBox(
                                expanded = intervalExpanded,
                                onExpandedChange = { if (!isSaving) intervalExpanded = !intervalExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = when (installmentInterval) {
                                        604800000L -> "Weekly (7 Days)"
                                        1209600000L -> "Bi-weekly (14 Days)"
                                        else -> "Monthly (30 Days)"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Installment Interval") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    enabled = !isSaving,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonBlue,
                                        unfocusedBorderColor = GlassEdge,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = intervalExpanded,
                                    onDismissRequest = { intervalExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Weekly (7 Days)") },
                                        onClick = {
                                            installmentInterval = 604800000L
                                            intervalExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Bi-weekly (14 Days)") },
                                        onClick = {
                                            installmentInterval = 1209600000L
                                            intervalExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Monthly (30 Days)") },
                                        onClick = {
                                            installmentInterval = 2592000000L
                                            intervalExpanded = false
                                        }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = amountPaid,
                                onValueChange = { amountPaid = it },
                                label = { Text("Amount Paid Now") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = !isSaving,
                                isError = isOverpaid || isNegativePaid,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonBlue,
                                    unfocusedBorderColor = GlassEdge,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                supportingText = {
                                    if (isOverpaid) {
                                        Text("Amount exceeds final total", color = NeonRed)
                                    } else if (isNegativePaid) {
                                        Text("Amount cannot be negative", color = NeonRed)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            item {
                // Summary Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GlassBg.copy(alpha = 0.4f))
                        .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Summary", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = NeonBlue, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { contentDescription = "summary_section" }
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Final Amount", color = TextSecondary)
                            Text("₹ %.2f".format(Locale.ENGLISH, finalAmount), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining", color = TextSecondary)
                            Text("₹ %.2f".format(Locale.ENGLISH, remainingAmount), color = NeonAmber, fontWeight = FontWeight.Bold)
                        }
                        if (paymentType == "Partial Payment") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Installment", color = TextSecondary)
                                Text("₹ %.2f".format(Locale.ENGLISH, installmentAmount), color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonBlue)
                }

                Button(
                    onClick = {
                        val customer = selectedCustomer
                        if (customer == null) {
                            Toast.makeText(context, "Please select a customer", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val finalCount = if (paymentType == "Partial Payment") {
                            (installmentCount.toIntOrNull() ?: 1).coerceAtLeast(1)
                        } else 1
                        
                        val finalAmountPaid = amountPaid.toDoubleOrNull() ?: 0.0
                        
                        if (vendorId.isEmpty()) {
                            Toast.makeText(context, "Vendor ID missing", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSaving = true
                        expanded = false

                        val finalRemaining = (finalAmount - finalAmountPaid).coerceAtLeast(0.0)
                        val finalInstallmentAmount = finalRemaining / finalCount

                        val installments = if (finalRemaining > 0.01) {
                            List(finalCount) { i ->
                                Installment(
                                    installmentId = UUID.randomUUID().toString(),
                                    saleId = "", 
                                    customerId = customer.customerId,
                                    vendorId = vendorId,
                                    dueDate = firstDueDate + (i * installmentInterval),
                                    amount = finalInstallmentAmount,
                                    amountPaid = 0.0,
                                    status = "PENDING"
                                )
                            }
                        } else emptyList()

                        viewModel.createSale(
                            itemName = itemName.trim(),
                            quantity = quantity.toIntOrNull() ?: 1,
                            unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                            totalAmount = totalAmount,
                            interestRate = interestPercent,
                            installmentCount = finalCount,
                            paymentType = paymentType,
                            amountPaid = finalAmountPaid,
                            customerId = customer.customerId,
                            customerName = customer.name,
                            vendorId = vendorId,
                            installments = installments
                        ) {
                            isSaving = false
                            view.post { onSaleCreated() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 4.dp)
                        .semantics { contentDescription = "submit_sale_button" },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    enabled = itemName.isNotBlank() && unitPrice.isNotBlank() && selectedCustomer != null && !isSaving && !isOverpaid && !isNegativePaid && !isInvalidCount && !isPastDate
                ) {
                    Text(
                        if (isSaving) "Creating Sale..." else "Create Sale",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
