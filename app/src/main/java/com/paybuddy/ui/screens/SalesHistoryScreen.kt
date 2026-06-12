package com.paybuddy.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paybuddy.data.model.Customer
import com.paybuddy.data.model.Sale
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.SalesViewModel
import com.paybuddy.viewmodel.SalesUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: SalesViewModel,
    vendorId: String,
    customers: List<Customer> = emptyList(),
    onAddSaleClick: () -> Unit,
    onSaleClick: (String) -> Unit
) {
    val uiState by viewModel.salesUiState.collectAsState()
    val activeSales by viewModel.activeSales.collectAsState()
    val historySales by viewModel.historySales.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active", "History")

    var saleToDelete by remember { mutableStateOf<Sale?>(null) }
    var saleToEdit by remember { mutableStateOf<Sale?>(null) }

    LaunchedEffect(vendorId) {
        Log.d("SalesHistoryScreen", "LaunchedEffect: Loading sales for $vendorId")
        viewModel.loadSales(vendorId)
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(success) {
        success?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                ),
                title = { Text("Sales History", fontWeight = FontWeight.ExtraBold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSaleClick,
                containerColor = NeonBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Sale")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = GlassBg.copy(alpha = 0.5f),
                contentColor = NeonBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
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
                                text = title, 
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTab == index) NeonBlue else TextSecondary,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is SalesUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is SalesUiState.Empty -> {
                        EmptyState(onAddSaleClick, selectedTab)
                    }
                    is SalesUiState.Success -> {
                        val currentList = if (selectedTab == 0) activeSales else historySales
                        
                        if (currentList.isEmpty()) {
                            EmptyState(onAddSaleClick, selectedTab)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(currentList, key = { it.saleId }) { sale ->
                                    SaleItem(
                                        sale = sale,
                                        onEditClick = { saleToEdit = sale },
                                        onDeleteClick = { saleToDelete = sale },
                                        onSaleClick = { onSaleClick(sale.saleId) }
                                    )
                                }
                            }
                        }
                    }
                    is SalesUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.loadSales(vendorId) }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    saleToDelete?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this sale?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSale(sale) {
                            saleToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Sale Dialog
    saleToEdit?.let { sale ->
        EditSaleDialog(
            sale = sale,
            customers = customers,
            onDismiss = { saleToEdit = null },
            onConfirm = { updatedSale ->
                viewModel.updateSale(sale, updatedSale) {
                    Toast.makeText(context, "Sale updated successfully", Toast.LENGTH_SHORT).show()
                }
                saleToEdit = null
            }
        )
    }
}

@Composable
private fun EmptyState(onAddSaleClick: () -> Unit, selectedTab: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val emptyMessage = if (selectedTab == 0) "No active sales" else "No completed sales"
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddSaleClick) {
                Text("Create Your First Sale")
            }
        }
    }
}

@Composable
fun SaleItem(
    sale: Sale,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaleClick: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg.copy(alpha = 0.4f))
            .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable { onSaleClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = sale.customerName.ifEmpty { "Unknown Customer" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onEditClick,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = NeonBlue)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = NeonRed)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${"%.0f".format(Locale.ENGLISH, sale.finalAmount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = NeonBlue,
                    fontWeight = FontWeight.ExtraBold
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sale.status != "COMPLETED") {
                val progress = (sale.amountPaid / sale.finalAmount).toFloat().coerceIn(0f, 1f)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Payment Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonBlue,
                        trackColor = GlassEdge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Paid: ₹${"%.0f".format(Locale.ENGLISH, sale.amountPaid)} / Total: ₹${"%.0f".format(Locale.ENGLISH, sale.finalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            } else {
                Text(
                    text = "Fully Paid: ₹${"%.0f".format(Locale.ENGLISH, sale.finalAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sdf.format(Date(sale.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSaleDialog(
    sale: Sale,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onConfirm: (Sale) -> Unit
) {
    var itemName by remember { mutableStateOf(sale.itemName) }
    var quantity by remember { mutableStateOf(sale.quantity.toString()) }
    var unitPrice by remember { mutableStateOf(sale.unitPrice.toString()) }
    var interestRate by remember { mutableStateOf(sale.interestRate.toString()) }
    var installmentCount by remember { mutableStateOf(sale.installmentCount.toString()) }
    var amountPaid by remember { mutableStateOf(sale.amountPaid.toString()) }
    var selectedCustomer by remember { mutableStateOf(customers.find { it.customerId == sale.customerId }) }
    var expanded by remember { mutableStateOf(false) }
    var paymentType by remember { mutableStateOf(sale.paymentType) }

    val totalAmount = (quantity.toDoubleOrNull() ?: 0.0) * (unitPrice.toDoubleOrNull() ?: 0.0)
    val interestAmount = if (paymentType == "Full Payment") 0.0 else (totalAmount * (interestRate.toDoubleOrNull() ?: 0.0) / 100)
    val finalAmount = totalAmount + interestAmount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize()
            ) {
                Text("Edit Sale", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: sale.customerName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Customer") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
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

                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("Item Name") },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = unitPrice,
                                onValueChange = { unitPrice = it },
                                label = { Text("Price") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        OutlinedTextField(
                            value = "₹ %.2f".format(Locale.ENGLISH, totalAmount),
                            onValueChange = {},
                            label = { Text("Total Amount") },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            readOnly = true
                        )

                        Text("Payment Type", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = paymentType == "Full Payment",
                                onClick = { 
                                    paymentType = "Full Payment"
                                    amountPaid = "%.2f".format(Locale.ENGLISH, totalAmount)
                                }
                            )
                            Text("Full")
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = paymentType == "Partial Payment",
                                onClick = { paymentType = "Partial Payment" }
                            )
                            Text("Partial")
                        }

                        if (paymentType == "Partial Payment") {
                            OutlinedTextField(
                                value = installmentCount,
                                onValueChange = { installmentCount = it },
                                label = { Text("Installment Count") },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = interestRate,
                                onValueChange = { interestRate = it },
                                label = { Text("Interest Rate (%)") },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = amountPaid,
                                onValueChange = { amountPaid = it },
                                label = { Text("Amount Paid") },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Final Amount (with interest): ₹ %.2f".format(Locale.ENGLISH, finalAmount))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val updatedSale = sale.copy(
                                itemName = itemName.trim(),
                                quantity = quantity.toIntOrNull() ?: 1,
                                unitPrice = unitPrice.toDoubleOrNull() ?: 0.0,
                                totalAmount = totalAmount,
                                interestRate = if (paymentType == "Full Payment") 0.0 else (interestRate.toDoubleOrNull() ?: 0.0),
                                installmentCount = installmentCount.toIntOrNull() ?: 1,
                                paymentType = paymentType,
                                amountPaid = amountPaid.toDoubleOrNull() ?: 0.0,
                                customerId = selectedCustomer?.customerId ?: sale.customerId,
                                customerName = selectedCustomer?.name ?: sale.customerName
                            )
                            onConfirm(updatedSale)
                        },
                        enabled = itemName.isNotBlank() && unitPrice.isNotBlank()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
