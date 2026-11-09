package com.example.trailmind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueFaint

private val TextDark = Color(0xFF1A1A2E)

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(TrailBlueFaint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TrailBlue, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(
    label: String,
    selected: String,
    options: List<String>,
    icon: ImageVector? = null,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = TrailBlue) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrailBlue,
                focusedLabelColor = TrailBlue
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = TrailBlue) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TrailBlue,
            focusedLabelColor = TrailBlue
        )
    )
}

@Composable
fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null,
                tint = if (enabled) TrailBlue else Color(0xFFBBBBBB),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 14.sp, color = if (enabled) TextDark else Color(0xFFAAAAAA))
                if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Color(0xFFAAAAAA))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = TrailBlue)
        )
    }
}

@Composable
fun ErrorBox(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFDECEA))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = Color(0xFFD32F2F), fontSize = 13.sp)
    }
}