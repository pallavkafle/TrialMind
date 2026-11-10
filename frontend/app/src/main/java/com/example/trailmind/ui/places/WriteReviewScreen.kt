package com.example.trailmind.ui.places

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.components.RatingPicker
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.util.Resource

private val TextMuted = Color(0xFF6B7280)
private val TextDark = Color(0xFF1A1A2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewScreen(
    name: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: WriteReviewViewModel = viewModel()
) {
    var rating by rememberSaveable { mutableStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val state by viewModel.state.collectAsState()
    val isLoading = state is Resource.Loading
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state) {
        if (state is Resource.Success) onDone()
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("Write a review", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
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
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(4.dp))
            Text("How was it? Your review helps other travellers.", fontSize = 13.sp, color = TextMuted)
            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Your rating", icon = Icons.Default.Star) {
                RatingPicker(rating = rating, onChange = { rating = it })
                Text(
                    if (rating == 0) "Tap a star" else "$rating out of 5",
                    fontSize = 12.sp, color = TextMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Your review", icon = Icons.Default.Edit) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 2000) text = it },
                    label = { Text("Share your experience") },
                    minLines = 5,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("${text.trim().length}/2000") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrailBlue,
                        focusedLabelColor = TrailBlue
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reviews are checked automatically for safety hazards such as landslides or floods. " +
                        "The first check can take up to a minute.",
                    fontSize = 12.sp, color = TextMuted
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val clean = text.trim()
                    localError = when {
                        rating == 0 -> "Tap a star to rate."
                        clean.length < 3 -> "Write at least 3 characters."
                        else -> null
                    }
                    if (localError == null) {
                        keyboard?.hide()
                        viewModel.submit(name, rating, clean)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrailBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Post review", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            localError?.let { ErrorBox(it) }
            (state as? Resource.Error)?.let { ErrorBox(it.message) }

            Spacer(Modifier.height(24.dp))
        }
    }
}