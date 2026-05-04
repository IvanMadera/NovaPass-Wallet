package com.example.novapass.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.novapass.ui.theme.*

@Composable
fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = NovaColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(NovaSpacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NovaColors.GlassLight, RoundedCornerShape(20.dp))
                .border(1.dp, NovaColors.BorderSubtle, RoundedCornerShape(20.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = NovaColors.TextSecondary, style = MaterialTheme.typography.bodyLarge) },
                shape = RoundedCornerShape(20.dp),
                readOnly = readOnly,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = NovaColors.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NovaColors.Transparent,
                    unfocusedContainerColor = NovaColors.Transparent,
                    disabledContainerColor = NovaColors.Transparent,
                    focusedBorderColor = NovaColors.Transparent,
                    unfocusedBorderColor = NovaColors.Transparent,
                    disabledBorderColor = NovaColors.Transparent,
                    cursorColor = NovaColors.GoldPrimary,
                    disabledTextColor = NovaColors.TextSecondary
                ),
                singleLine = true
            )
            // Overlay invisible para interceptar el clic si es readOnly y tiene onClick
            if (onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onClick() }
                )
            }
        }
    }
}
