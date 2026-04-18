package com.example.novapass.ui.components

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
                .clip(RoundedCornerShape(20.dp))
                .drawBehind {
                    // Reflejo superior para efecto "tallado"
                    drawRect(NovaBrushes.GlassTopLight)
                }
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.03f), // Borde fantasma
                    RoundedCornerShape(20.dp)
                )
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder, color = NovaColors.TextSecondary.copy(alpha = 0.4f)) },
                shape = RoundedCornerShape(20.dp),
                readOnly = readOnly,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NovaColors.GoldPrimary.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = NovaColors.GlassMedium,
                    unfocusedContainerColor = NovaColors.GlassLight,
                    disabledBorderColor = Color.Transparent,
                    disabledContainerColor = NovaColors.GlassLight.copy(alpha = 0.5f),
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
