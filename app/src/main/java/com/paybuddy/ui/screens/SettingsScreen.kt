package com.paybuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybuddy.ui.components.DeveloperOptionsSection
import com.paybuddy.ui.components.ResetAppDataDialog
import com.paybuddy.ui.settings.SettingsViewModel
import com.paybuddy.ui.theme.*
import com.paybuddy.utils.Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onBusinessProfileClick: () -> Unit,
    onSecurityNotificationsClick: () -> Unit,
    onArchivedRecordsClick: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsState()
    val showResetDialog by viewModel.showResetDialog.collectAsState()
    val isResetting by viewModel.isResetting.collectAsState()
    val resetSuccess by viewModel.resetSuccess.collectAsState()
    val isLoggingOut by viewModel.isLoggingOut.collectAsState()
    val logoutSuccess by viewModel.logoutSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(logoutSuccess, resetSuccess) {
        if (logoutSuccess || resetSuccess) {
            onLogoutSuccess()
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(text = "Error", fontWeight = FontWeight.Bold) },
            text = { Text(text = errorMessage ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK", color = NeonRed, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BackgroundDark,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLogoutDialog() },
            title = { Text(text = "Logout", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to logout?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Logout", color = NeonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLogoutDialog() }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = BackgroundDark,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showResetDialog) {
        ResetAppDataDialog(
            onDismiss = { viewModel.dismissResetDialog() },
            onConfirm = { viewModel.confirmReset() },
            isLoading = isResetting
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(20.dp)
        ) {
            Text(
                text = "ACCOUNT & PROFILE",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassBg.copy(alpha = 0.7f))
                    .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
            ) {
                Column {
                    SettingsItem(
                        title = "Business Profile",
                        icon = Icons.Rounded.Person,
                        onClick = onBusinessProfileClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Security & Notifications",
                        icon = Icons.Rounded.Notifications,
                        onClick = onSecurityNotificationsClick
                    )
                    SettingsDivider()
                    SettingsItem(
                        title = "Archived Records",
                        icon = Icons.AutoMirrored.Rounded.List,
                        onClick = onArchivedRecordsClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SYSTEM",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassBg.copy(alpha = 0.7f))
                    .border(1.dp, GlassEdge, RoundedCornerShape(24.dp))
            ) {
                Column {
                    SettingsItem(
                        title = "Logout",
                        icon = Icons.AutoMirrored.Rounded.ExitToApp,
                        titleColor = if (isLoggingOut) TextSecondary else NeonRed,
                        iconColor = if (isLoggingOut) TextSecondary else NeonRed,
                        isBold = true,
                        onClick = { viewModel.onLogoutClick() }
                    )
                }
            }

            if (isLoggingOut) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = NeonRed
                )
            }

            if (Config.ENABLE_DEVELOPER_RESET) {
                Spacer(modifier = Modifier.height(32.dp))
                DeveloperOptionsSection(
                    onResetClick = { viewModel.onResetClick() }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Version 1.1.0 (BETA)",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    icon: ImageVector,
    titleColor: Color = Color.White,
    iconColor: Color = NeonBlue,
    isBold: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            color = titleColor,
            fontSize = 16.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = GlassEdge.copy(alpha = 0.3f)
    )
}
