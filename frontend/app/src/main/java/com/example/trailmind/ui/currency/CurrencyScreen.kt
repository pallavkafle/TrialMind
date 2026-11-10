package com.example.trailmind.ui.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.CurrencyRatesDto
import com.example.trailmind.ui.components.Dropdown
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueFaint
import com.example.trailmind.util.Resource
import com.example.trailmind.util.formatRfcDate

private val TextMuted = Color(0xFF6B7280)
private val TextDark = Color(0xFF1A1A2E)

// small rates (like yen) need more decimals to be readable
private fun rateText(v: Double): String = if (v < 10) "%.4f".format(v) else "%,.2f".format(v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    onBack: () -> Unit,
    viewModel: CurrencyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var amount by rememberSaveable { mutableStateOf("1000") }
    var foreignToNpr by rememberSaveable { mutableStateOf(false) }   // false = NPR to foreign
    var code by rememberSaveable { mutableStateOf("USD") }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("Currency", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrailBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            when (val s = state) {
                null, is Resource.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is Resource.Error -> {
                    ErrorBox(s.message)
                    TextButton(onClick = { viewModel.load(force = true) }) { Text("Retry") }
                }

                is Resource.Success -> RatesContent(
                    d = s.data,
                    amount = amount, onAmount = { amount = it },
                    foreignToNpr = foreignToNpr, onDirection = { foreignToNpr = it },
                    code = code, onCode = { code = it }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RatesContent(
    d: CurrencyRatesDto,
    amount: String, onAmount: (String) -> Unit,
    foreignToNpr: Boolean, onDirection: (Boolean) -> Unit,
    code: String, onCode: (String) -> Unit
) {
    val rates = d.rates.orEmpty()
    if (rates.isEmpty()) {
        ErrorBox("No rates are available right now.")
        return
    }
    val uriHandler = LocalUriHandler.current
    val selected = rates.firstOrNull { it.code == code } ?: rates.first()
    val value = amount.toDoubleOrNull() ?: 0.0
    val result = if (foreignToNpr) value * selected.npr_per_unit else value / selected.npr_per_unit

    SectionCard(title = "Converter", icon = Icons.Default.CurrencyExchange) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !foreignToNpr,
                onClick = { onDirection(false) },
                label = { Text("NPR → ${selected.code}", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TrailBlueFaint,
                    selectedLabelColor = TrailBlue
                )
            )
            FilterChip(
                selected = foreignToNpr,
                onClick = { onDirection(true) },
                label = { Text("${selected.code} → NPR", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TrailBlueFaint,
                    selectedLabelColor = TrailBlue
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { v ->
                if (v.length <= 12 && v.count { it == '.' } <= 1 && v.all { it.isDigit() || it == '.' }) onAmount(v)
            },
            label = { Text(if (foreignToNpr) "Amount in ${selected.code}" else "Amount in NPR") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrailBlue,
                focusedLabelColor = TrailBlue
            )
        )

        Spacer(Modifier.height(12.dp))

        Dropdown(
            "Currency",
            "${selected.code} · ${selected.name}",
            rates.map { "${it.code} · ${it.name}" },
            Icons.Default.Public
        ) { onCode(it.substringBefore(" ·")) }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TrailBlueFaint)
                .padding(16.dp)
        ) {
            Text(
                if (foreignToNpr) "NPR ${"%,.2f".format(result)}"
                else "${"%,.2f".format(result)} ${selected.code}",
                fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TrailBlue
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "1 ${selected.code} = NPR ${rateText(selected.npr_per_unit)}",
                fontSize = 12.sp, color = TextMuted
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    SectionCard(title = "Today's rates", icon = Icons.Default.Info) {
        rates.forEachIndexed { i, r ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${r.quote_unit} ${r.code}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(r.name, fontSize = 11.sp, color = TextMuted)
                }
                Text(
                    "NPR ${"%,.2f".format(r.npr_for_quote_unit)}",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TrailBlue
                )
            }
            if (i < rates.lastIndex) HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }

    Spacer(Modifier.height(12.dp))

    val updated = formatRfcDate(d.updated_at)
    if (updated.isNotEmpty()) Text("Rates updated $updated.", fontSize = 12.sp, color = TextMuted)
    d.note?.let { Text(it, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp)) }
    d.budget_rate_usd_npr?.let {
        Text(
            "TrailMind budget estimates use NPR ${"%.0f".format(it)} per US dollar.",
            fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp)
        )
    }
    if (d.stale == true) {
        Text(
            "Showing saved rates; the rate service could not be reached just now.",
            fontSize = 12.sp, color = Color(0xFFB26A00), modifier = Modifier.padding(top = 4.dp)
        )
    }
    val url = d.attribution_url
    Text(
        "Rates by Exchange Rate API",
        fontSize = 12.sp, color = TrailBlue, textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .padding(top = 6.dp)
            .clickable {
                if (url != null) {
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        // no browser available: ignore
                    }
                }
            }
    )
}