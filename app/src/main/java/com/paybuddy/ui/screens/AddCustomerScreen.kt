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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.Customer
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.CustomerViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    viewModel: CustomerViewModel,
    vendorId: String,
    onCustomerAdded: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val error by viewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            isSaving = false
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Add New Customer", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassBg.copy(alpha = 0.7f))
                    .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Customer Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            if (nameError != null && it.isNotBlank()) nameError = null
                        },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "customer_name_input" },
                        enabled = !isSaving,
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it, color = NeonRed) } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = GlassEdge,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = NeonBlue,
                            unfocusedLabelColor = TextSecondary
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                phone = it
                                if (phoneError != null && it.length >= 10) phoneError = null
                            }
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "customer_phone_input" },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        enabled = !isSaving,
                        isError = phoneError != null,
                        supportingText = phoneError?.let { { Text(it, color = NeonRed) } },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonBlue,
                            unfocusedBorderColor = GlassEdge,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = NeonBlue,
                            unfocusedLabelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Manual validation
                    var hasError = false
                    if (name.isBlank()) {
                        nameError = "Name is required"
                        hasError = true
                    }
                    if (phone.length < 10) {
                        phoneError = "Enter a valid 10-digit number"
                        hasError = true
                    }
                    
                    if (hasError) return@Button

                    if (vendorId.isBlank()) {
                        Toast.makeText(context, "User session expired. Please login again.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSaving = true
                    val customer = Customer(
                        customerId = UUID.randomUUID().toString(),
                        name = name.trim(),
                        phone = phone.trim(),
                        vendorId = vendorId,
                        totalAmount = 0.0,
                        paidAmount = 0.0,
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.addCustomer(customer) {
                        isSaving = false
                        onCustomerAdded()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp)
                    .semantics { contentDescription = "submit_customer_button" },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Saving...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Save Customer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
