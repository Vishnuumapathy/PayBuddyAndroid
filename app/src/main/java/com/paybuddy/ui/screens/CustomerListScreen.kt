package com.paybuddy.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paybuddy.data.model.Customer
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.CustomerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    viewModel: CustomerViewModel,
    vendorId: String,
    onCustomerClick: (String) -> Unit,
    onAddCustomerClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val customers by viewModel.getCustomers(vendorId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }

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

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Customers", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                containerColor = NeonBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassBg.copy(alpha = 0.5f)),
                    placeholder = { Text("Search customers...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassEdge,
                        cursorColor = NeonBlue,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (filteredCustomers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No customers yet" else "No matching customers found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredCustomers, key = { it.customerId }) { customer ->
                            CustomerItem(
                                customer = customer,
                                onClick = { 
                                    if (customer.customerId.isNotEmpty()) {
                                        onCustomerClick(customer.customerId)
                                    }
                                },
                                onEditClick = { customerToEdit = customer },
                                onDeleteClick = { customerToDelete = customer }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = NeonBlue
                )
            }
        }
    }

    // Delete Confirmation Dialog
    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("Delete Customer") },
            text = { Text("Are you sure you want to delete ${customer.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomer(vendorId, customer.customerId) {
                            customerToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Customer Dialog
    customerToEdit?.let { customer ->
        EditCustomerDialog(
            customer = customer,
            onDismiss = { customerToEdit = null },
            onSave = { updatedName, updatedPhone ->
                val updatedCustomer = customer.copy(name = updatedName, phone = updatedPhone)
                viewModel.updateCustomer(updatedCustomer) {
                    customerToEdit = null
                }
            }
        )
    }
}

@Composable
fun EditCustomerDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Customer") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Name") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError) {
                    Text("Name cannot be empty", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { 
                        phone = it
                        phoneError = it.isBlank()
                    },
                    label = { Text("Phone") },
                    isError = phoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (phoneError) {
                    Text("Phone cannot be empty", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(name, phone)
                    } else {
                        nameError = name.isBlank()
                        phoneError = phone.isBlank()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CustomerItem(
    customer: Customer, 
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg.copy(alpha = 0.7f))
            .border(1.dp, GlassEdge, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customer.name.ifEmpty { "Unknown Customer" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Customer",
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Customer",
                            tint = NeonRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total: ₹ ${"%.2f".format(Locale.ENGLISH, customer.totalAmount)}", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Paid: ₹ ${"%.2f".format(Locale.ENGLISH, customer.paidAmount)}", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Remaining", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = "₹ ${"%.2f".format(Locale.ENGLISH, customer.remainingBalance)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (customer.remainingBalance > 0) NeonAmber else NeonGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
