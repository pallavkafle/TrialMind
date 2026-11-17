package com.example.trailmind.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.local.ServerConfig
import com.example.trailmind.data.local.TokenStore
import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.theme.Pal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BAD = "Enter a valid address, for example https://example.com"

@Composable
fun ServerAddressCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = ApiClient.baseUrl
    var text by remember { mutableStateOf(ServerConfig.saved(context).orEmpty()) }
    var status by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    SectionCard(title = "Server address", icon = Icons.Default.Cloud) {
        Text("In use: $current", fontSize = 12.sp, color = Pal.textMuted)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; status = null },
            label = { Text("Server address") },
            placeholder = { Text(ServerConfig.PUBLIC_URL) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = !testing,
                onClick = {
                    val target = ServerConfig.normalize(text.ifBlank { current })
                    if (target == null) {
                        status = BAD
                    } else {
                        testing = true
                        status = "Testing..."
                        scope.launch {
                            status = withContext(Dispatchers.IO) { ServerConfig.test(target) }
                            testing = false
                        }
                    }
                }
            ) { Text("Test connection") }
            Button(onClick = {
                val target = ServerConfig.normalize(text)
                when {
                    target == null -> status = BAD
                    target == current -> status = "Already using this address"
                    else -> confirm = target
                }
            }) { Text("Save") }
        }
        TextButton(onClick = {
            if (ServerConfig.saved(context) == null) status = "Already using the default address" else confirmReset = true
        }) { Text("Reset to default") }
        status?.let {
            Text(it, fontSize = 12.sp, color = if (it.startsWith("Connected")) Pal.green else Pal.textBody)
        }
    }

    confirm?.let { target ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Switch server?") },
            text = { Text("You will be logged out and saved data will be cleared. The app then restarts and connects to:\n$target") },
            confirmButton = {
                TextButton(onClick = { confirm = null; switchServer(context, scope, target) }) { Text("Switch") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Use the default address?") },
            text = { Text("You will be logged out and saved data will be cleared. The app then restarts.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; switchServer(context, scope, null) }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

// a different server has different users: save the address, log out, clear saved data, restart the app process
private fun switchServer(context: Context, scope: CoroutineScope, newUrl: String?) {
    scope.launch {
        ServerConfig.save(context, newUrl)
        TokenStore.get(context).clear()      // also clears the offline cache
        delay(600)                           // let the clean-up finish before the process ends
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(it)
        }
        Runtime.getRuntime().exit(0)
    }
}