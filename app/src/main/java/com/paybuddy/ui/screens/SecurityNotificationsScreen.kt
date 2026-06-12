package com.paybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityNotificationsScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = { Text("Security & Notifications", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecurityItem(
                title = "Daily Reminders",
                description = "Receive notifications for overdue payments",
                checked = true
            )
            SecurityItem(
                title = "Marketing Notifications",
                description = "Stay updated with new features",
                checked = false
            )
            SecurityItem(
                title = "Biometric Lock",
                description = "Secure your data with fingerprint or face ID",
                checked = false
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Security Info",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassBg.copy(alpha = 0.3f))
                    .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Your data is encrypted and securely stored in Cloud Firestore. We use Firebase Auth for secure login.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SecurityItem(
    title: String,
    description: String,
    checked: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg.copy(alpha = 0.4f))
            .border(0.5.dp, GlassEdge.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { /* Placeholder */ },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeonBlue,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = GlassEdge
                )
            )
        }
    }
}
