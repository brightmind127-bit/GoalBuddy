package com.goalbuddy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goalbuddy.app.avatar.AvatarOptions

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val pal = LocalPalette.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(pal.corner))
            .background(pal.card)
            .padding(14.dp)
    ) {
        Text(title, color = pal.onCard, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    val pal = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = pal.onCard, fontSize = 15.sp)
            if (subtitle != null) Text(subtitle, color = pal.sub, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    label: (Float) -> String = { "%.1f".format(it) },
    onChange: (Float) -> Unit
) {
    val pal = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = pal.onCard, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(label(value), color = pal.sub, fontSize = 13.sp)
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(title: String?, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val pal = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (title != null) Text(title, color = pal.onCard, fontSize = 15.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { i, o ->
                FilterChip(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    label = { Text(o, fontSize = 13.sp) }
                )
            }
        }
    }
}

/** A row of color swatches + a 🎨 custom color picker. */
@Composable
fun ColorRow(title: String, color: Long, palette: List<Long> = AvatarOptions.vivid, onChange: (Long) -> Unit) {
    val pal = LocalPalette.current
    var showPicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = pal.onCard, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color.toColor())
                    .border(1.dp, pal.onCard.copy(alpha = 0.3f), CircleShape)
            )
        }
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                        .clickable { showPicker = true },
                    contentAlignment = Alignment.Center
                ) { Text("🎨", fontSize = 14.sp) }
            }
            items(palette) { c ->
                val sel = c == color
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(c.toColor())
                        .border(if (sel) 3.dp else 1.dp, if (sel) pal.accent else pal.onCard.copy(alpha = 0.25f), CircleShape)
                        .clickable { onChange(c) }
                )
            }
        }
    }
    if (showPicker) {
        ColorPickerDialog(initial = color, onDismiss = { showPicker = false }, onPick = { onChange(it); showPicker = false })
    }
}

@Composable
fun ColorPickerDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val hsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toInt(), it) }
    }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    var a by remember { mutableFloatStateOf(((initial ushr 24) and 0xFF).toFloat() / 255f) }
    val argb = android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(argb.toLong() and 0xFFFFFFFFL) }) { Text("Use color") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Pick a color") },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(argb))
                )
                Spacer(Modifier.height(8.dp))
                Text("Hue")
                Slider(value = h, onValueChange = { h = it }, valueRange = 0f..360f)
                Text("Saturation")
                Slider(value = s, onValueChange = { s = it }, valueRange = 0f..1f)
                Text("Brightness")
                Slider(value = v, onValueChange = { v = it }, valueRange = 0f..1f)
                Text("Opacity")
                Slider(value = a, onValueChange = { a = it }, valueRange = 0.1f..1f)
            }
        }
    )
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    val pal = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("←", fontSize = 22.sp, color = pal.onBg) }
        Text(title, color = pal.onBg, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        actions()
        Spacer(Modifier.width(4.dp))
    }
}
