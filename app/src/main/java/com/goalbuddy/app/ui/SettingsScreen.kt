package com.goalbuddy.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.goalbuddy.app.data.AppSettings
import com.goalbuddy.app.data.AppState
import com.goalbuddy.app.data.ParticleConfig
import com.goalbuddy.app.data.Period
import com.goalbuddy.app.data.Store

private val bgPalette = listOf(
    0xFFF3EEFF, 0xFFFFE9F3, 0xFFE6FFF6, 0xFFE3F4FF, 0xFFFFF1E0, 0xFFFFF8E1, 0xFFFAFAF7, 0xFFFFFFFF,
    0xFF0F1027, 0xFF2A1B4A, 0xFF03253F, 0xFF005B8C, 0xFF0E1F14, 0xFF1E3A26, 0xFF000000, 0xFF212121
)

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit, onParticles: () -> Unit) {
    val pal = LocalPalette.current
    val st = state.settings
    val ctx = LocalContext.current
    fun set(s: AppSettings) = Store.setSettings(s)
    var confirm by remember { mutableStateOf<String?>(null) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        set(Store.current.settings.copy(lockNotif = granted))
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            SectionCard("🎨 Look & feel") {
                ChoiceChips("Theme", themePresets.map { it.name }, st.themePreset) { set(st.applyPreset(it)) }
                ColorRow("Accent color", st.accent) { set(st.copy(accent = it)) }
                ChoiceChips("Background", listOf("Solid", "Gradient", "Gradient + dots", "Gradient + grid"), st.bgStyle) { set(st.copy(bgStyle = it)) }
                ColorRow("Background color 1", st.bgColor1, bgPalette) { set(st.copy(bgColor1 = it)) }
                if (st.bgStyle > 0) ColorRow("Background color 2", st.bgColor2, bgPalette) { set(st.copy(bgColor2 = it)) }
                ColorRow("Card color", st.cardColor, bgPalette) { set(st.copy(cardColor = it)) }
                SliderRow("Card opacity", st.cardOpacity, 0.2f..1f, label = { "${(it * 100).toInt()}%" }) { set(st.copy(cardOpacity = it)) }
                SliderRow("Corner roundness", st.cornerRadius.toFloat(), 0f..32f, label = { "${it.toInt()} dp" }) { set(st.copy(cornerRadius = it.toInt())) }
                SliderRow("Text size", st.fontScale, 0.8f..1.4f, label = { "${(it * 100).toInt()}%" }) { set(st.copy(fontScale = it)) }
                SwitchRow("Compact list", st.compact, "Smaller rows, hides notes & tags") { set(st.copy(compact = it)) }
                SwitchRow("Emoji on tabs", st.tabEmoji) { set(st.copy(tabEmoji = it)) }
                ChoiceChips("Open the app on", Period.entries.map { it.label }, st.startPage) { set(st.copy(startPage = it)) }
            }

            SectionCard("✅ Goal list") {
                SwitchRow("Hide completed goals", st.hideCompleted, "Done goals disappear from the list") { set(st.copy(hideCompleted = it)) }
                if (st.hideCompleted) {
                    SliderRow("Disappear after", st.hideDelaySec.toFloat(), 0f..10f, steps = 9, label = { "${it.toInt()} s" }) { set(st.copy(hideDelaySec = it.toInt())) }
                }
                SwitchRow("Move completed to the bottom", st.completedToBottom) { set(st.copy(completedToBottom = it)) }
                SwitchRow("Strike-through completed", st.strikeCompleted) { set(st.copy(strikeCompleted = it)) }
                SwitchRow("Fade completed", st.dimCompleted) { set(st.copy(dimCompleted = it)) }
                ChoiceChips("Sort by", listOf("Oldest first", "Newest first", "Priority", "A → Z"), st.sortMode) { set(st.copy(sortMode = it)) }
                ChoiceChips("Checkbox style", listOf("▢ Square", "◯ Circle", "✅ Emoji", "⭐ Star", "💖 Heart"), st.checkboxStyle) { set(st.copy(checkboxStyle = it)) }
                SwitchRow("Progress bar", st.showProgress) { set(st.copy(showProgress = it)) }
                SwitchRow("Show counts & %", st.showCounts) { set(st.copy(showCounts = it)) }
                SwitchRow("Show notes", st.showNotes) { set(st.copy(showNotes = it)) }
                SwitchRow("◀ ▶ Browse other days/weeks…", st.showPeriodNav) { set(st.copy(showPeriodNav = it)) }
                SwitchRow("Carry unfinished goals forward", st.carryOver, "Unchecked goals move to the next day/week/month/year") { set(st.copy(carryOver = it)) }
                SwitchRow("Ask before deleting", st.confirmDelete) { set(st.copy(confirmDelete = it)) }
                SwitchRow("Confetti when a goal is done 🎉", st.celebrate) { set(st.copy(celebrate = it)) }
                SwitchRow("Vibrate on check", st.haptics) { set(st.copy(haptics = it)) }
            }

            SectionCard("🧸 Buddy behaviour") {
                SwitchRow("Show my buddy", st.avatarEnabled) { set(st.copy(avatarEnabled = it)) }
                SliderRow("Size", st.avatarSize.toFloat(), 50f..140f, label = { "${it.toInt()} dp" }) { set(st.copy(avatarSize = it.toInt())) }
                SliderRow("Walking speed", st.avatarSpeed, 0.2f..3f, label = { "×%.1f".format(it) }) { set(st.copy(avatarSpeed = it)) }
                ChoiceChips("How chatty", listOf("🤐 Rarely", "🙂 Normal", "😄 Chatty", "🗣️ Non-stop"), st.chatFrequency) { set(st.copy(chatFrequency = it)) }
                SliderRow("Bubble stays for", st.bubbleSeconds, 1.5f..10f, label = { "%.1f s".format(it) }) { set(st.copy(bubbleSeconds = it)) }
                ChoiceChips("Bubble side", listOf("Auto", "Left", "Right"), st.bubbleSide) { set(st.copy(bubbleSide = it)) }
                Text("Edges my buddy can walk on", color = pal.onCard, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                SwitchRow("⬇ Bottom", st.edgeBottom) { set(st.copy(edgeBottom = it)) }
                SwitchRow("⬆ Top (upside-down!)", st.edgeTop) { set(st.copy(edgeTop = it)) }
                SwitchRow("⬅ Left wall", st.edgeLeft) { set(st.copy(edgeLeft = it)) }
                SwitchRow("➡ Right wall", st.edgeRight) { set(st.copy(edgeRight = it)) }
                SwitchRow("Talk when I tap it", st.reactTaps) { set(st.copy(reactTaps = it)) }
                SwitchRow("React to scrolling", st.reactScroll) { set(st.copy(reactScroll = it)) }
                SwitchRow("Eyes follow my finger", st.eyesFollowTouch) { set(st.copy(eyesFollowTouch = it)) }
                Text("What it talks about", color = pal.onCard, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
                SwitchRow("😂 Funny lines", st.phraseFunny) { set(st.copy(phraseFunny = it)) }
                SwitchRow("💪 Motivation", st.phraseMotivation) { set(st.copy(phraseMotivation = it)) }
                SwitchRow("🎯 Reminders about my goals", st.phraseGoals) { set(st.copy(phraseGoals = it)) }
                SwitchRow("💡 Tips", st.phraseFacts) { set(st.copy(phraseFacts = it)) }
            }

            SectionCard("✨ Particles") {
                SwitchRow("Particles on", state.particles.enabled) { Store.setParticles(state.particles.copy(enabled = it)) }
                OutlinedButton(onClick = onParticles, modifier = Modifier.fillMaxWidth()) { Text("Customize particles…") }
            }

            SectionCard("📱 Widgets") {
                Text(
                    "Long-press your home screen → Widgets → Goal Buddy. There are two: the Goals widget (checkboxes, tabs, + button to plan) and the Avatar widget.",
                    color = pal.sub, fontSize = 13.sp
                )
                ChoiceChips("Widget starts on", Period.entries.map { it.label }, st.widgetPeriod) { set(st.copy(widgetPeriod = it)) }
                SwitchRow("Show completed goals in widget", st.widgetShowDone) { set(st.copy(widgetShowDone = it)) }
                SwitchRow("Show buddy in widget", st.widgetShowAvatar) { set(st.copy(widgetShowAvatar = it)) }
                SwitchRow("Dark widget", st.widgetDark) { set(st.copy(widgetDark = it)) }
                SliderRow("Widget opacity", st.widgetOpacity, 0.2f..1f, label = { "${(it * 100).toInt()}%" }) { set(st.copy(widgetOpacity = it)) }
            }

            SectionCard("🔒 Lock screen") {
                Text(
                    "Shows a live goal card on your lock screen with ✓ Done and + Add buttons. " +
                        "(On tablets / Android versions that support lock-screen widgets you can also add the Goal Buddy widget there.)",
                    color = pal.sub, fontSize = 13.sp
                )
                SwitchRow("Goals on lock screen", st.lockNotif) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else set(st.copy(lockNotif = on))
                }
                ChoiceChips("Show goals for", Period.entries.map { it.label }, st.lockPeriod) { set(st.copy(lockPeriod = it)) }
                SwitchRow("Include completed goals", st.lockShowDone) { set(st.copy(lockShowDone = it)) }
                SwitchRow("Buddy's line on the card", st.lockAvatarLine) { set(st.copy(lockAvatarLine = it)) }
            }

            SectionCard("🗂 Data") {
                OutlinedButton(onClick = { confirm = "settings" }, modifier = Modifier.fillMaxWidth()) { Text("Reset look & settings") }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { confirm = "goals" }, modifier = Modifier.fillMaxWidth()) { Text("Delete ALL goals") }
            }
        }
    }

    confirm?.let { what ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (what == "goals") "Delete all goals?" else "Reset settings?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    if (what == "goals") Store.resetGoals() else Store.resetSettings()
                    confirm = null
                }) { Text("Yes") }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ParticleSettingsScreen(state: AppState, onBack: () -> Unit) {
    val p = state.particles
    fun set(n: ParticleConfig) = Store.setParticles(n)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Particles ✨", onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            SectionCard("Ambient particles") {
                SwitchRow("Enabled", p.enabled) { set(p.copy(enabled = it)) }
                ChoiceChips("Shape", ParticleOptions.shapes, p.shape) { set(p.copy(shape = it)) }
                ChoiceChips("Movement", ParticleOptions.motions, p.motion) { set(p.copy(motion = it)) }
                SliderRow("Amount", p.count.toFloat(), 0f..120f, label = { "${it.toInt()}" }) { set(p.copy(count = it.toInt())) }
                SliderRow("Speed", p.speed, 0.1f..3f, label = { "×%.1f".format(it) }) { set(p.copy(speed = it)) }
                SliderRow("Smallest size", p.sizeMin, 1f..20f, label = { "${it.toInt()}" }) { set(p.copy(sizeMin = it, sizeMax = maxOf(it, p.sizeMax))) }
                SliderRow("Biggest size", p.sizeMax, 1f..30f, label = { "${it.toInt()}" }) { set(p.copy(sizeMax = it, sizeMin = minOf(it, p.sizeMin))) }
                SliderRow("Opacity", p.opacity, 0.05f..1f, label = { "${(it * 100).toInt()}%" }) { set(p.copy(opacity = it)) }
                SliderRow("Life time", p.lifetime, 1f..15f, label = { "%.1f s".format(it) }) { set(p.copy(lifetime = it)) }
                SliderRow("Wind ←  → ", p.wind, -1f..1f, label = { "%.1f".format(it) }) { set(p.copy(wind = it)) }
                SwitchRow("Appear & disappear (fade)", p.fadeInOut) { set(p.copy(fadeInOut = it)) }
                SwitchRow("Spin", p.spin) { set(p.copy(spin = it)) }
                SwitchRow("Glow", p.glow) { set(p.copy(glow = it)) }
                SwitchRow("In front of the list", p.inFront, "Off = behind the cards") { set(p.copy(inFront = it)) }
            }
            SectionCard("Colors") {
                ChoiceChips(null, ParticleOptions.colorModes, p.colorMode) { set(p.copy(colorMode = it)) }
                if (p.colorMode == 3) {
                    val cols = p.customColors + List((3 - p.customColors.size).coerceAtLeast(0)) { 0xFFFFFFFF }
                    cols.take(3).forEachIndexed { i, c ->
                        ColorRow("Color ${i + 1}", c) { new ->
                            val l = cols.take(3).toMutableList(); l[i] = new
                            set(p.copy(customColors = l))
                        }
                    }
                }
            }
            SectionCard("Celebrations") {
                SwitchRow("Confetti burst when a goal is done", p.burstOnDone) { set(p.copy(burstOnDone = it)) }
                SliderRow("Confetti amount", p.burstCount.toFloat(), 10f..150f, label = { "${it.toInt()}" }) { set(p.copy(burstCount = it.toInt())) }
                SwitchRow("Sparkles where I touch", p.tapSparkles) { set(p.copy(tapSparkles = it)) }
            }
        }
    }
}
