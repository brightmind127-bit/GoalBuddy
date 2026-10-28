package com.goalbuddy.app.widget

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goalbuddy.app.avatar.AvatarPose
import com.goalbuddy.app.avatar.Mood
import com.goalbuddy.app.avatar.Phrases
import com.goalbuddy.app.data.Period
import com.goalbuddy.app.data.Periods
import com.goalbuddy.app.data.Store
import com.goalbuddy.app.ui.AvatarView
import com.goalbuddy.app.ui.FancyCheckbox
import com.goalbuddy.app.ui.GoalTheme
import com.goalbuddy.app.ui.LocalPalette

/**
 * The small "planning window" that opens on top of the home screen (or lock screen)
 * from the widget / lock-screen card: add goals, tick them off, delete them.
 */
class QuickAddActivity : ComponentActivity() {
    companion object { const val EXTRA_PERIOD = "period" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true)
        Store.init(this)
        val start = intent.getIntExtra(EXTRA_PERIOD, Store.current.settings.widgetPeriod).coerceIn(0, 3)
        setContent {
            val state by Store.state.collectAsState()
            GoalTheme(state.settings) {
                QuickAddPanel(start, onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun QuickAddPanel(startPeriod: Int, onClose: () -> Unit) {
    val state by Store.state.collectAsState()
    val pal = LocalPalette.current
    var periodIdx by remember { mutableIntStateOf(startPeriod) }
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(0) }
    var repeat by remember { mutableStateOf(false) }
    val period = Period.entries[periodIdx]
    val key = Periods.currentKey(period)
    val goals = Store.arrange(Store.goalsFor(state, period, key), state.settings.copy(hideCompleted = false))
    var line by remember { mutableStateOf(Phrases.pick(Mood.GREET, state.settings, state.avatar.personality, state.avatar.name)) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun add() {
        if (text.isBlank()) return
        Store.addGoal(text, period, key, priority, "", repeat)
        line = Phrases.pick(Mood.GOAL_ADDED, state.settings, state.avatar.personality, state.avatar.name, text.trim())
        text = ""; priority = 0; repeat = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(18.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(if (pal.dark) Color(0xFF1F1C2B) else Color.White)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarView(
                    state.avatar,
                    AvatarPose(expression = 1, armR = 150f, shadow = false),
                    Modifier.size(54.dp, 76.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(pal.accent.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) { Text(line, fontSize = 14.sp, color = if (pal.dark) Color.White else Color(0xFF1E1B2E)) }
            }
            Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Period.entries.forEachIndexed { i, p ->
                    FilterChip(selected = i == periodIdx, onClick = { periodIdx = i }, label = { Text(p.label) })
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(120) },
                placeholder = { Text("New goal for ${Periods.title(period, 0).lowercase()}…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() })
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = priority == 1, onClick = { priority = if (priority == 1) 0 else 1 }, label = { Text("⭐") })
                FilterChip(selected = priority == 2, onClick = { priority = if (priority == 2) 0 else 2 }, label = { Text("🔥") })
                FilterChip(selected = repeat, onClick = { repeat = !repeat }, label = { Text("🔁 Repeat") })
            }
            Text(
                "${Periods.title(period, 0)} · ${goals.count { it.done }}/${goals.size} done",
                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                color = if (pal.dark) Color.White else Color(0xFF1E1B2E),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                items(goals, key = { it.id }) { g ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).animateItem(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FancyCheckbox(g.done, state.settings.checkboxStyle, pal.accent, 24) {
                            if (Store.toggle(g.id)) {
                                line = Phrases.pick(Mood.GOAL_DONE, state.settings, state.avatar.personality, state.avatar.name, g.title)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (if (g.emoji.isNotEmpty()) g.emoji + " " else "") + g.title,
                            modifier = Modifier.weight(1f),
                            color = (if (pal.dark) Color.White else Color(0xFF1E1B2E)).copy(alpha = if (g.done) 0.5f else 1f),
                            textDecoration = if (g.done) TextDecoration.LineThrough else TextDecoration.None
                        )
                        TextButton(onClick = { Store.delete(g.id) }) { Text("✕", color = Color(0xFFE53935)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text("Close") }
                Spacer(Modifier.width(6.dp))
                Button(onClick = { add() }) { Text("Add") }
            }
        }
    }
}
