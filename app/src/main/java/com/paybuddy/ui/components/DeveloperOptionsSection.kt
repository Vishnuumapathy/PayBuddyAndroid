package com.paybuddy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeveloperOptionsSection(
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Developer Options",
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0x1AFFFFFF),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onResetClick)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Reset App Data",
                    color = Color(0xFFFB7185), // neon_red
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
