package com.example.trailmind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val StarYellow = Color(0xFFFFC107)

// read-only stars, rounded to the nearest whole star
@Composable
fun RatingStars(rating: Double, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val filled = rating.roundToInt().coerceIn(0, 5)
    Row(modifier) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= filled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = StarYellow,
                modifier = Modifier.size(size)
            )
        }
    }
}

// tappable stars for choosing a rating
@Composable
fun RatingPicker(rating: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier) {
        for (i in 1..5) {
            IconButton(onClick = { onChange(i) }) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (i == 1) "1 star" else "$i stars",
                    tint = StarYellow,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// small rounded label in a given colour
@Composable
fun InfoTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 11.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}