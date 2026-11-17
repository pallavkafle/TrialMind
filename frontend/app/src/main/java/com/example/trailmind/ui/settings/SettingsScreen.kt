package com.example.trailmind.ui.settings

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.local.OfflineCache
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.theme.Pal
import com.example.trailmind.ui.theme.ThemePrefs

private data class Credit(val name: String, val text: String, val url: String?)

private val credits = listOf(
    Credit(
        "OpenStreetMap",
        "Map tiles, hospital and clinic locations, hotels and walking paths. © OpenStreetMap contributors (ODbL licence).",
        "https://www.openstreetmap.org/copyright"
    ),
    Credit(
        "Open-Meteo",
        "Weather forecasts. Weather data by Open-Meteo.com (CC BY 4.0).",
        "https://open-meteo.com"
    ),
    Credit(
        "Exchange Rate API",
        "Currency rates (open access, updated once a day).",
        "https://www.exchangerate-api.com"
    ),
    Credit(
        "Google Gemini",
        "Writes the day-by-day itinerary text and helps read free-text trip requests. " +
            "Distances, costs and permits are calculated by the app.",
        "https://ai.google.dev"
    ),
    Credit(
        "DistilBERT (Hugging Face)",
        "Checks traveller reviews for sentiment and safety hazards.",
        "https://huggingface.co"
    ),
    Credit(
        "Permit and entry fees",
        "Researched from public sources and can change. Always confirm with the park, office or agency before you travel.",
        null
    )
)

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    "${info.versionName} ($code)"
} catch (e: Exception) {
    "1.0"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val mode = ThemePrefs.mode
    var confirmClear by remember { mutableStateOf(false) }
    val version = remember { appVersion(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
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
            SectionCard(title = "Appearance", icon = Icons.Default.Palette) {
                ThemeOption("Light", mode == ThemePrefs.LIGHT) { ThemePrefs.setMode(context, ThemePrefs.LIGHT) }
                ThemeOption("Dark", mode == ThemePrefs.DARK) { ThemePrefs.setMode(context, ThemePrefs.DARK) }
                ThemeOption("Follow my phone", mode == ThemePrefs.SYSTEM) { ThemePrefs.setMode(context, ThemePrefs.SYSTEM) }
                Spacer(Modifier.height(4.dp))
                Text(
                    "You can also switch quickly with the sun/moon button on the Home screen.",
                    fontSize = 12.sp, color = Pal.textMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Saved data", icon = Icons.Default.CloudOff) {
                Text(
                    "The app keeps a copy of places, weather, rates and trips you have viewed, so they still show " +
                        "without internet. Clearing frees space; the data comes back the next time you are online.",
                    fontSize = 13.sp, color = Pal.textBody
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { confirmClear = true }) { Text("Clear saved data") }
            }

            Spacer(Modifier.height(14.dp))

            ServerAddressCard()

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Data sources and credits", icon = Icons.Default.Public) {
                credits.forEachIndexed { i, c ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (c.url != null) {
                                    Modifier.clickable {
                                        try {
                                            uriHandler.openUri(c.url)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No web browser found on this device.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            c.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (c.url != null) Pal.blue else Pal.textDark,
                            textDecoration = if (c.url != null) TextDecoration.Underline else null
                        )
                        Text(c.text, fontSize = 12.sp, color = Pal.textMuted)
                    }
                    if (i < credits.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "About", icon = Icons.Default.Info) {
                Text("TrailMind", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pal.textDark)
                Text("Intelligent Travel Planning for Nepal", fontSize = 12.sp, color = Pal.textMuted)
                Spacer(Modifier.height(6.dp))
                Text("Version $version", fontSize = 12.sp, color = Pal.textMuted)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear saved data?") },
            text = { Text("Places, weather, rates and trips will need internet to load again.") },
            confirmButton = {
                TextButton(onClick = {
                    OfflineCache.clearAsync()
                    confirmClear = false
                    Toast.makeText(context, "Saved data cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear", color = Pal.red) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = Pal.textDark)
    }
}