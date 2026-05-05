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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .background(NovaColors.GlassLight, RoundedCornerShape(16.dp))
                .border(1.dp, NovaColors.BorderSubtle, RoundedCornerShape(16.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 18.dp) // Más aire: 18dp
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                readOnly = readOnly,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = NovaColors.White, fontSize = 14.sp),
                cursorBrush = SolidColor(NovaColors.GoldPrimary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = NovaColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }
                    innerTextField()
                }
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
