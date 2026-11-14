package com.example.trailmind.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.ui.components.CountryField
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.theme.Pal
import com.example.trailmind.util.Resource
import com.example.trailmind.util.nationalityCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val saveState by viewModel.save.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current

    var name by rememberSaveable { mutableStateOf(user?.name.orEmpty()) }
    var country by rememberSaveable { mutableStateOf(user?.nationality ?: "NP") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(saveState) {
        if (saveState is Resource.Success) {
            snackbar.showSnackbar("Profile saved")
            viewModel.clearSave()
        }
    }

    val saving = saveState is Resource.Loading
    val cleanName = name.trim()
    val changed = cleanName != user?.name || country != user?.nationality

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Pal.blueFaint),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = user?.name?.trim()?.firstOrNull()?.uppercaseChar()
                        if (initial != null) {
                            Text(initial.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Pal.blue)
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Pal.blue)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(user?.name ?: "Traveller", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Pal.textDark)
                        Text(user?.email.orEmpty(), fontSize = 13.sp, color = Pal.textMuted)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Your details", icon = Icons.Default.Person) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Full name") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrailBlue,
                        focusedLabelColor = TrailBlue
                    )
                )
                Spacer(Modifier.height(12.dp))
                CountryField(code = country, onChange = { country = it })
                Spacer(Modifier.height(8.dp))
                Text(
                    "Budgets and permits will use ${nationalityCategory(country)} prices.",
                    fontSize = 12.sp, color = Pal.textMuted
                )
                Text("Your email cannot be changed.", fontSize = 12.sp, color = Pal.textMuted)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (cleanName.length !in 2..60) {
                        localError = "Name must be 2 to 60 characters."
                    } else {
                        localError = null
                        keyboard?.hide()
                        viewModel.save(cleanName, country)
                    }
                },
                enabled = changed && !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrailBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Save changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(14.dp))
            localError?.let { ErrorBox(it) }
            (saveState as? Resource.Error)?.let { ErrorBox(it.message) }
            Spacer(Modifier.height(24.dp))
        }
    }
}