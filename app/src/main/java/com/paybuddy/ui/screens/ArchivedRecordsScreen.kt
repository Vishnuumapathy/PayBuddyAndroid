package com.paybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.data.model.Customer
import com.paybuddy.data.model.Sale
import com.paybuddy.ui.theme.*
import com.paybuddy.viewmodel.CustomerViewModel
import com.paybuddy.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedRecordsScreen(
    vendorId: String,
    customerViewModel: CustomerViewModel,
    salesViewModel: SalesViewModel,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Customers", "Sales")
    val archivedCustomers by customerViewModel.getArchivedCustomers(vendorId).collectAsState(initial = emptyList())
    val archivedSales by salesViewModel.getArchivedSales(vendorId).collectAsState(initial = emptyList())
    val isProcessing = customerViewModel.isLoading.collectAsState().value || salesViewModel.isProcessing.collectAsState().value
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Archived Records", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = NeonRed, indicator = { tabPositions -> if (selectedTab < tabPositions.size) SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = NeonRed, height = 3.dp) }, divider = { HorizontalDivider(color = GlassEdge, thickness = 0.5.dp) }) {
                tabs.forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(text = title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == index) NeonRed else TextSecondary) }) }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) ArchivedCustomersList(customers = archivedCustomers, onRestore = { id -> if (!isProcessing) customerViewModel.restoreCustomer(id) {} })
                else ArchivedSalesList(sales = archivedSales, onRestore = { id -> if (!isProcessing) salesViewModel.restoreSale(id) {} })
                if (isProcessing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = NeonRed, trackColor = Color.Transparent)
            }
        }
    }
}

@Composable
private fun ArchivedCustomersList(customers: List<Customer>, onRestore: (String) -> Unit) {
    if (customers.isEmpty()) EmptyState("No archived customers")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(customers, key = { it.customerId }) { customer ->
            ArchivedItemCard(title = customer.name, subtitle = if (customer.phone.isNotEmpty()) "Phone: ${customer.phone}" else "No phone", date = customer.archivedAt, onRestore = { onRestore(customer.customerId) })
        }
    }
}

@Composable
private fun ArchivedSalesList(sales: List<Sale>, onRestore: (String) -> Unit) {
    if (sales.isEmpty()) EmptyState("No archived sales")
    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(sales, key = { it.saleId }) { sale ->
            ArchivedItemCard(title = sale.itemName, subtitle = "Customer: ${sale.customerName}\nAmount: ₹${"%.2f".format(Locale.getDefault(), sale.finalAmount)}\nStatus: ${sale.status}", date = sale.archivedAt, onRestore = { onRestore(sale.saleId) })
        }
    }
}

@Composable
private fun ArchivedItemCard(title: String, subtitle: String, date: Long?, onRestore: () -> Unit) {
    val df = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GlassBg.copy(alpha = 0.4f)).border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                Text(text = "Archived on: ${if (date != null && date > 0) df.format(Date(date)) else "N/A"}", color = TextSecondary.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Button(onClick = onRestore, colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.1f), contentColor = NeonRed), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed.copy(alpha = 0.5f)), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), modifier = Modifier.padding(start = 8.dp)) { Text("Restore", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun EmptyState(msg: String) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = msg, color = TextSecondary) } }
