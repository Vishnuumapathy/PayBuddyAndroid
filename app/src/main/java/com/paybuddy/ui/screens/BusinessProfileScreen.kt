package com.paybuddy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.Vendor
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.VendorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    vendor: Vendor?,
    viewModel: VendorViewModel,
    onBackClick: () -> Unit
) {
    val isEditing by viewModel.isEditing.collectAsState()
    val editName by viewModel.editName.collectAsState()
    val editShopName by viewModel.editShopName.collectAsState()
    val editPhone by viewModel.editPhone.collectAsState()
    val editUpiId by viewModel.editUpiId.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(updateState) {
        when (updateState) {
            is VendorViewModel.UpdateState.Success -> {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                viewModel.clearUpdateState()
            }
            is VendorViewModel.UpdateState.Error -> {
                Toast.makeText(context, (updateState as VendorViewModel.UpdateState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.clearUpdateState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = { Text("Business Profile", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (vendor != null) {
                        if (isEditing) {
                            IconButton(onClick = { viewModel.cancelEditing() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                            }
                            IconButton(
                                onClick = { viewModel.updateVendorProfile() },
                                enabled = updateState !is VendorViewModel.UpdateState.Loading
                            ) {
                                if (updateState is VendorViewModel.UpdateState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NeonBlue, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.Check, contentDescription = "Save", tint = NeonGreen)
                                }
                            }
                        } else {
                            IconButton(onClick = { viewModel.startEditing() }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = NeonBlue)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            if (vendor == null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else {
                if (isEditing) {
                    EditField(label = "Vendor Name", value = editName, onValueChange = { viewModel.onNameChange(it) })
                    EditField(label = "Shop Name", value = editShopName, onValueChange = { viewModel.onShopNameChange(it) })
                    EditField(label = "Phone Number", value = editPhone, onValueChange = { viewModel.onPhoneChange(it) }, keyboardType = KeyboardType.Phone)
                    EditField(label = "UPI ID (Optional)", value = editUpiId, onValueChange = { viewModel.onUpiIdChange(it) })
                } else {
                    ProfileInfoCard(label = "Vendor Name", value = vendor.name)
                    ProfileInfoCard(label = "Shop Name", value = vendor.shopName)
                    ProfileInfoCard(label = "Phone Number", value = vendor.phone)
                    ProfileInfoCard(label = "UPI ID", value = vendor.upiId)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = GlassBg.copy(alpha = 0.5f),
                unfocusedContainerColor = GlassBg.copy(alpha = 0.3f),
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = GlassEdge,
                cursorColor = NeonBlue
            ),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
        )
    }
}

@Composable
private fun ProfileInfoCard(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg.copy(alpha = 0.4f))
            .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (value.isBlank()) "Not set" else value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
