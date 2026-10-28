package com.goalbuddy.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goalbuddy.app.data.AppSettings
import com.goalbuddy.app.data.AppState
import com.goalbuddy.app.data.AvatarEvent
import com.goalbuddy.app.data.Events
import com.goalbuddy.app.data.Goal
import com.goalbuddy.app.data.Period
import com.goalbuddy.app.data.Periods
import com.goalbuddy.app.data.Store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Where the content box sits on screen, so confetti can start exactly at a checkbox. */
object RootOrigin { var offset = Offset.Zero }

/** The offset (in periods) currently shown on each page, so the avatar can talk about it. */
object PageOffsets { val values = IntArray(4) }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: AppState,
    pagerState: PagerState,
    onAvatar: () -> Unit,
    onParticles: () -> Unit,
    onSettings: () -> Unit
) {
    val pal = LocalPalette.current
    val scope = rememberCoroutineScope()
    val st = state.settings

    Column(Modifier.fillMaxSize()) {
        // ---- top bar
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Goal Buddy", color = pal.onBg, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("with ${state.avatar.name.ifBlank { "Buddy" }}", color = pal.onBg.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            TextButton(onClick = onAvatar) { Text("🎨", fontSize = 22.sp) }
            TextButton(onClick = onParticles) { Text("✨", fontSize = 22.sp) }
            TextButton(onClick = onSettings) { Text("⚙️", fontSize = 22.sp) }
        }

        // ---- tabs (Day / Week / Month / Year); swipe the pages left/right too
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = pal.onBg
        ) {
            Period.entries.forEachIndexed { i, p ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = { Text(if (st.tabEmoji) "${p.emoji} ${p.label}" else p.label, fontSize = 13.sp, maxLines = 1) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            PeriodPage(Period.entries[page], state)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeriodPage(period: Period, state: AppState) {
    val pal = LocalPalette.current
    val st = state.settings
    var offset by rememberSaveable(period) { mutableIntStateOf(0) }
    PageOffsets.values[period.ordinal] = offset
    val key = Periods.keyFor(period, offset)
    val all = Store.goalsFor(state, period, key)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var editing by remember { mutableStateOf<Goal?>(null) }

    // tick while recently completed goals are waiting to disappear
    LaunchedEffect(all, st.hideCompleted, st.hideDelaySec) {
        now = System.currentTimeMillis()
        while (st.hideCompleted && all.any { it.done && System.currentTimeMillis() - it.doneAt < st.hideDelaySec * 1000L + 300 }) {
            delay(200)
            now = System.currentTimeMillis()
        }
    }
    val list = Store.arrange(all, st, now)
    val doneCount = all.count { it.done }
    val bottomPad = if (st.avatarEnabled && st.edgeBottom) (st.avatarSize * 1.25f).dp else 24.dp

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(if (st.compact) 4.dp else 8.dp)
    ) {
        item(key = "header") {
            PeriodHeader(period, offset, doneCount, all.size, st,
                onPrev = { offset-- }, onNext = { offset++ }, onNow = { offset = 0 })
        }
        item(key = "add") { AddGoalBar(period, key, offset, st) }
        items(list, key = { it.id }) { g ->
            GoalRow(
                goal = g, st = st,
                modifier = Modifier.animateItem(),
                onToggle = { origin -> toggleGoal(g, origin, period, key, st) },
                onEdit = { editing = g }
            )
        }
        if (all.isEmpty()) {
            item(key = "empty") {
                Text(
                    "No goals here yet.\nAdd one above ✨",
                    color = pal.onBg.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else if (st.hideCompleted && doneCount > 0) {
            item(key = "hidden") {
                Text(
                    "✓ $doneCount completed goal${if (doneCount > 1) "s" else ""} hidden",
                    color = pal.onBg.copy(alpha = 0.55f), fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        if (all.isNotEmpty()) {
            item(key = "footer") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { Store.clearDone(period, key) }) { Text("🧹 Clear done", color = pal.onBg) }
                    TextButton(onClick = { Store.uncheckAll(period, key) }) { Text("↺ Uncheck all", color = pal.onBg) }
                }
            }
        }
    }

    editing?.let { g ->
        GoalEditDialog(g, st, onDismiss = { editing = null })
    }
}

private fun toggleGoal(g: Goal, origin: Offset?, period: Period, key: String, st: AppSettings) {
    val nowDone = Store.toggle(g.id)
    if (nowDone) {
        val remaining = Store.goalsFor(Store.current, period, key).count { !it.done }
        if (st.celebrate) Events.burst.tryEmit(origin?.let { it - RootOrigin.offset })
        Events.avatar.tryEmit(AvatarEvent.GoalDone(g.title, remaining == 0))
    }
}

@Composable
private fun PeriodHeader(
    period: Period, offset: Int, done: Int, total: Int, st: AppSettings,
    onPrev: () -> Unit, onNext: () -> Unit, onNow: () -> Unit
) {
    val pal = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(pal.corner))
            .background(pal.card)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (st.showPeriodNav) TextButton(onClick = onPrev) { Text("◀", color = pal.onCard) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Periods.title(period, offset), color = pal.onCard, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(Periods.subtitle(period, offset), color = pal.sub, fontSize = 12.sp)
            }
            if (st.showPeriodNav) TextButton(onClick = onNext) { Text("▶", color = pal.onCard) }
        }
        if (offset != 0) {
            TextButton(onClick = onNow, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("↩ Back to ${if (period == Period.DAY) "today" else "this ${period.label.lowercase()}"}")
            }
        }
        if (st.showProgress && total > 0) {
            Spacer(Modifier.height(6.dp))
            val frac by animateFloatAsState(done.toFloat() / total, label = "progress")
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = pal.accent,
                trackColor = pal.accent.copy(alpha = 0.18f)
            )
            if (st.showCounts) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("$done of $total done", color = pal.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("${(frac * 100).toInt()}%", color = pal.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (offset == 0 && st.showProgress) {
            val passed = Periods.elapsedFraction(period)
            Text(
                "⏳ ${(passed * 100).toInt()}% of the ${period.label.lowercase()} has passed",
                color = pal.sub, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddGoalBar(period: Period, key: String, offset: Int, st: AppSettings) {
    val pal = LocalPalette.current
    val focus = LocalFocusManager.current
    var text by rememberSaveable(key) { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(0) }
    var repeat by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf("") }
    fun add() {
        if (text.isBlank()) return
        Store.addGoal(text, period, key, priority, emoji, repeat)
        Events.avatar.tryEmit(AvatarEvent.GoalAdded(text.trim()))
        text = ""; priority = 0; repeat = false; emoji = ""
    }
    val hint = when (period) {
        Period.DAY -> if (offset == 0) "Add a goal for today…" else "Add a goal for this day…"
        Period.WEEK -> "Add a goal for this week…"
        Period.MONTH -> "Add a goal for this month…"
        Period.YEAR -> "Add a goal for this year…"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(pal.corner))
            .background(pal.card)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(120) },
                placeholder = { Text(hint) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() })
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { add(); focus.clearFocus() }, shape = CircleShape) { Text("+", fontSize = 20.sp) }
        }
        if (text.isNotBlank()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                FilterChip(selected = priority == 1, onClick = { priority = if (priority == 1) 0 else 1 }, label = { Text("⭐ Important") })
                FilterChip(selected = priority == 2, onClick = { priority = if (priority == 2) 0 else 2 }, label = { Text("🔥 Urgent") })
                FilterChip(selected = repeat, onClick = { repeat = !repeat }, label = { Text("🔁 Every ${period.label.lowercase()}") })
                for (e in listOf("💪", "📚", "💧", "🏃", "🧘", "💼", "🎨", "🧹")) {
                    FilterChip(selected = emoji == e, onClick = { emoji = if (emoji == e) "" else e }, label = { Text(e) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoalRow(goal: Goal, st: AppSettings, modifier: Modifier, onToggle: (Offset?) -> Unit, onEdit: () -> Unit) {
    val pal = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    var checkPos by remember { mutableStateOf<Offset?>(null) }
    val dim = goal.done && st.dimCompleted
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(pal.corner))
            .background(pal.card)
            .combinedClickable(onClick = onEdit, onLongClick = onEdit)
            .padding(horizontal = 10.dp, vertical = if (st.compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (goal.priority > 0) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (goal.priority == 2) Color(0xFFFF5252) else Color(0xFFFFC107))
            )
            Spacer(Modifier.width(6.dp))
        }
        Box(Modifier.onGloballyPositioned { c ->
            val p = c.positionInRoot()
            checkPos = Offset(p.x + c.size.width / 2f, p.y + c.size.height / 2f)
        }) {
            FancyCheckbox(goal.done, st.checkboxStyle, pal.accent) {
                if (st.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle(checkPos)
            }
        }
        Spacer(Modifier.width(10.dp))
        if (goal.emoji.isNotEmpty()) {
            Text(goal.emoji, fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                goal.title,
                color = pal.onCard.copy(alpha = if (dim) 0.45f else 1f),
                fontSize = if (st.compact) 14.sp else 16.sp,
                textDecoration = if (goal.done && st.strikeCompleted) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (st.showNotes && goal.note.isNotBlank() && !st.compact) {
                Text(goal.note, color = pal.sub, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val tags = buildList {
                if (goal.priority == 1) add("⭐ Important")
                if (goal.priority == 2) add("🔥 Urgent")
                if (goal.repeating) add("🔁 Repeats")
                if (goal.carried > 0) add("↪ Carried ×${goal.carried}")
            }
            if (tags.isNotEmpty() && !st.compact) {
                Text(tags.joinToString("  "), color = pal.sub, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun FancyCheckbox(checked: Boolean, style: Int, accent: Color, size: Int = 26, onClick: () -> Unit) {
    val pal = LocalPalette.current
    val scale by animateFloatAsState(if (checked) 1f else 0.92f, spring(dampingRatio = 0.35f), label = "check")
    val mod = Modifier
        .size(size.dp)
        .scale(scale)
        .clip(if (style == 1) CircleShape else RoundedCornerShape(7.dp))
        .clickable(onClick = onClick)
    when (style) {
        2 -> Box(mod, contentAlignment = Alignment.Center) { Text(if (checked) "✅" else "⬜", fontSize = (size - 6).sp) }
        3 -> Box(mod, contentAlignment = Alignment.Center) { Text(if (checked) "⭐" else "☆", fontSize = (size - 6).sp, color = pal.onCard) }
        4 -> Box(mod, contentAlignment = Alignment.Center) { Text(if (checked) "💖" else "🤍", fontSize = (size - 8).sp) }
        else -> Box(
            mod
                .background(if (checked) accent else Color.Transparent)
                .border(2.dp, if (checked) accent else pal.onCard.copy(alpha = 0.45f), if (style == 1) CircleShape else RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Text("✓", color = Color.White, fontSize = (size - 10).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalEditDialog(goal: Goal, st: AppSettings, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(goal.title) }
    var note by remember { mutableStateOf(goal.note) }
    var priority by remember { mutableIntStateOf(goal.priority) }
    var repeat by remember { mutableStateOf(goal.repeating) }
    var emoji by remember { mutableStateOf(goal.emoji) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete goal?") },
            text = { Text("\"${goal.title}\" will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    Store.delete(goal.id); Events.avatar.tryEmit(AvatarEvent.GoalDeleted); onDismiss()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit goal") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it.take(120) }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = note, onValueChange = { note = it.take(300) }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = priority == 0, onClick = { priority = 0 }, label = { Text("Normal") })
                    FilterChip(selected = priority == 1, onClick = { priority = 1 }, label = { Text("⭐ Important") })
                    FilterChip(selected = priority == 2, onClick = { priority = 2 }, label = { Text("🔥 Urgent") })
                    FilterChip(selected = repeat, onClick = { repeat = !repeat }, label = { Text("🔁 Repeat every ${goal.period.label.lowercase()}") })
                }
                Text("Icon", modifier = Modifier.padding(top = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (e in listOf("", "💪", "📚", "💧", "🏃", "🧘", "💼", "🎨", "🧹", "🍎", "💤", "💰", "🎵", "✈️", "❤️", "🎯", "🧠", "🌱")) {
                        FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e.ifEmpty { "none" }) })
                    }
                }
                Row(Modifier.padding(top = 6.dp)) {
                    TextButton(onClick = {
                        Store.moveGoal(goal.id, prevKey(goal)); onDismiss()
                    }) { Text("◀ Move back") }
                    TextButton(onClick = {
                        Store.moveGoal(goal.id, nextKey(goal)); onDismiss()
                    }) { Text("Move to next ▶") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    Store.saveGoal(goal.copy(title = title.trim(), note = note.trim(), priority = priority, repeating = repeat, emoji = emoji))
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (st.confirmDelete) confirmDelete = true
                else { Store.delete(goal.id); Events.avatar.tryEmit(AvatarEvent.GoalDeleted); onDismiss() }
            }) { Text("🗑 Delete", color = Color(0xFFE53935)) }
        }
    )
}

private fun dateOfKey(goal: Goal): java.time.LocalDate {
    // find the offset that produces this key (search near today)
    for (o in -400..400) {
        if (Periods.keyFor(goal.period, o) == goal.key) return Periods.dateFor(goal.period, o)
    }
    return java.time.LocalDate.now()
}

private fun nextKey(goal: Goal): String {
    val d = dateOfKey(goal)
    return Periods.key(goal.period, Periods.dateFor(goal.period, 1, d))
}

private fun prevKey(goal: Goal): String {
    val d = dateOfKey(goal)
    return Periods.key(goal.period, Periods.dateFor(goal.period, -1, d))
}
