package com.paybuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paybuddy.viewmodel.VendorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: VendorViewModel,
    vendorId: String,
    initialEmail: String = "",
    initialPhone: String = "",
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var vendorName by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf(initialEmail) }
    var upiId by remember { mutableStateOf("") }

    val state by viewModel.onboardingState.collectAsState()

    LaunchedEffect(state) {
        if (state is VendorViewModel.OnboardingState.Success) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendor Onboarding") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = vendorName,
                onValueChange = { vendorName = it },
                label = { Text("Vendor Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Shop Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("UPI ID (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            if (state is VendorViewModel.OnboardingState.Error) {
                Text(
                    text = (state as VendorViewModel.OnboardingState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    viewModel.saveVendorProfile(
                        vendorId = vendorId,
                        name = vendorName,
                        shopName = shopName,
                        phone = phone,
                        email = email,
                        upiId = upiId
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = vendorName.isNotEmpty() && shopName.isNotEmpty() && state !is VendorViewModel.OnboardingState.Loading
            ) {
                if (state is VendorViewModel.OnboardingState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save & Continue")
                }
            }
        }
    }
}
