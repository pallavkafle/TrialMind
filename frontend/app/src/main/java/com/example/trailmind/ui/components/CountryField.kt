package com.example.trailmind.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.ui.home.TrailBlue
import java.util.Locale

fun countryName(code: String): String =
    if (code.length != 2) code
    else Locale("", code).getDisplayCountry(Locale.ENGLISH).ifBlank { code }

// every country on the device, Nepal first, the rest A to Z
private val allCountries: List<Pair<String, String>> by lazy {
    val all = Locale.getISOCountries().map { it to countryName(it) }.sortedBy { it.second }
    all.filter { it.first == "NP" } + all.filter { it.first != "NP" }
}

// a read-only field that opens a searchable country list; code is the 2-letter ISO code (NP, IN, DE...)
@Composable
fun CountryField(
    code: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Nationality"
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = countryName(code),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = TrailBlue) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrailBlue,
                focusedLabelColor = TrailBlue
            )
        )
        // a see-through layer on top, so a tap anywhere on the field opens the list
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        CountryPickerDialog(
            selected = code,
            onPick = {
                onChange(it)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}

@Composable
private fun CountryPickerDialog(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) allCountries else allCountries.filter { it.second.contains(q, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your country") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(shown, key = { it.first }) { (c, n) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(n, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (c == selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = TrailBlue)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}