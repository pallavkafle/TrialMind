package com.example.trailmind.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueFaint
import com.example.trailmind.util.Resource

private val categoryOrder = listOf("Cultural", "Trekking", "Hiking", "Homestay", "Wildlife")

// destination picker grouped by category; selection lives in the caller's list
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DestinationChips(
    destinations: Resource<List<DestinationDto>>?,
    selected: SnapshotStateList<String>,
    maxSelected: Int,
    onRetry: () -> Unit
) {
    when (val d = destinations) {
        is Resource.Loading, null -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        is Resource.Error -> {
            ErrorBox(d.message)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        is Resource.Success -> {
            val byCategory = d.data.groupBy { it.category }
            categoryOrder.forEach { cat ->
                val items = byCategory[cat].orEmpty()
                if (items.isNotEmpty()) {
                    Text(cat, fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEach { dest ->
                            val isOn = dest.name in selected
                            FilterChip(
                                selected = isOn,
                                onClick = {
                                    if (isOn) selected.remove(dest.name)
                                    else if (selected.size < maxSelected) selected.add(dest.name)
                                },
                                label = { Text(dest.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TrailBlueFaint,
                                    selectedLabelColor = TrailBlue
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Text("Selected ${selected.size}/$maxSelected", fontSize = 12.sp, color = Color(0xFF6B7280))
        }
    }
}