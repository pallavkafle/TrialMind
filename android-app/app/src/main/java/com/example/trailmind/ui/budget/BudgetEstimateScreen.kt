package com.example.trailmind.ui.budget

import com.example.trailmind.network.RetrofitClient
import com.example.trailmind.network.BudgetRequest
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val knownRegions = listOf(
    "Annapurna", "Khumbu", "Langtang", "Manaslu",
    "Upper Mustang", "Kanchanjunga", "Lower Dolpa", "Humla", "Pokhara"
)
val knownPurposes = listOf("Trekking", "Holiday", "Pilgrimage", "Other")
val knownTiers = listOf("budget", "mid", "luxury")
val knownTransport = listOf("bus", "jeep", "flight", "trek")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEstimateScreen(onBack: () -> Unit) {
    var region by remember { mutableStateOf(knownRegions[1]) }
    var purpose by remember { mutableStateOf(knownPurposes[0]) }
    var tier by remember { mutableStateOf(knownTiers[1]) }
    var transport by remember { mutableStateOf(knownTransport[2]) }
    var days by remember { mutableStateOf("14") }
    var groupSize by remember { mutableStateOf("2") }

    var result by remember { mutableStateOf("Fill in details and tap estimate") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var regionExpanded by remember { mutableStateOf(false) }
    var purposeExpanded by remember { mutableStateOf(false) }
    var tierExpanded by remember { mutableStateOf(false) }
    var transportExpanded by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedTextColor = Color.Black,
        unfocusedLabelColor = Color.DarkGray,
        focusedLabelColor = Color.Black,
        disabledLabelColor = Color.DarkGray,
        unfocusedBorderColor = Color.Gray,
        focusedBorderColor = Color(0xFF1F5C8B)
    )

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(text = "TrailMind Budget Estimator", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(expanded = regionExpanded, onExpandedChange = { regionExpanded = it }) {
            OutlinedTextField(
                value = region, onValueChange = {}, readOnly = true,
                label = { Text("Region") }, colors = fieldColors,
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                knownRegions.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = { region = it; regionExpanded = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = purposeExpanded, onExpandedChange = { purposeExpanded = it }) {
            OutlinedTextField(
                value = purpose, onValueChange = {}, readOnly = true,
                label = { Text("Purpose") }, colors = fieldColors,
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = purposeExpanded, onDismissRequest = { purposeExpanded = false }) {
                knownPurposes.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = { purpose = it; purposeExpanded = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = days, onValueChange = { days = it },
                label = { Text("Days") }, colors = fieldColors, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = groupSize, onValueChange = { groupSize = it },
                label = { Text("Group Size") }, colors = fieldColors, modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = tierExpanded, onExpandedChange = { tierExpanded = it }) {
            OutlinedTextField(
                value = tier, onValueChange = {}, readOnly = true,
                label = { Text("Accommodation Tier") }, colors = fieldColors,
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = tierExpanded, onDismissRequest = { tierExpanded = false }) {
                knownTiers.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = { tier = it; tierExpanded = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = transportExpanded, onExpandedChange = { transportExpanded = it }) {
            OutlinedTextField(
                value = transport, onValueChange = {}, readOnly = true,
                label = { Text("Transport Mode") }, colors = fieldColors,
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = transportExpanded, onDismissRequest = { transportExpanded = false }) {
                knownTransport.forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = { transport = it; transportExpanded = false })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = result, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            loading = true
            result = "Loading..."
            scope.launch {
                try {
                    val response = RetrofitClient.api.predictBudget(
                        BudgetRequest(
                            region = region,
                            purpose = purpose,
                            month = 10,
                            days = days.toIntOrNull() ?: 14,
                            group_size = groupSize.toIntOrNull() ?: 2,
                            accommodation_tier = tier,
                            transport_mode = transport
                        )
                    )
                    result = "Estimated cost: NPR ${response.predicted_cost_npr}\n" +
                            "Per person: NPR ${response.per_person}"
                } catch (e: Exception) {
                    result = "Error: ${e.message}"
                } finally {
                    loading = false
                }
            }
        }) {
            Text(if (loading) "Loading..." else "Get Budget Estimate")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text("Back to Home")
        }
    }
}