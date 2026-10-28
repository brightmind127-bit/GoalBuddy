package com.goalbuddy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goalbuddy.app.avatar.AvatarOptions
import com.goalbuddy.app.avatar.Mood
import com.goalbuddy.app.avatar.Phrases
import com.goalbuddy.app.data.AppState
import com.goalbuddy.app.data.AvatarConfig
import com.goalbuddy.app.data.Store
import kotlinx.coroutines.delay

private val categories = listOf("🧍 Body", "🙂 Face", "👀 Eyes", "👄 Mouth", "💇 Hair", "🎩 Hats & Glasses", "👕 Outfit", "🎒 Extras", "✏️ Style")

@Composable
fun AvatarEditorScreen(state: AppState, onDone: () -> Unit) {
    val pal = LocalPalette.current
    var cfg by remember { mutableStateOf(state.avatar) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expression by remember { mutableIntStateOf(0) }
    var line by remember { mutableStateOf("Hi! Make me look awesome ✨") }
    var talking by remember { mutableStateOf(true) }

    // save (debounced) whenever something changes
    LaunchedEffect(cfg) {
        delay(350)
        Store.setAvatar(cfg)
    }
    LaunchedEffect(line) {
        talking = true
        delay(1200)
        talking = false
    }

    fun set(c: AvatarConfig) { cfg = c }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (state.firstRun) "Create your buddy" else "Your buddy",
            onBack = { Store.setAvatar(cfg); Store.update { it.copy(firstRun = false) }; onDone() }
        ) {
            TextButton(onClick = {
                cfg = AvatarOptions.random(cfg.name)
                line = Phrases.pick(Mood.TAP, state.settings, cfg.personality, cfg.name)
            }) { Text("🎲 Random", color = pal.onBg) }
        }

        // ---- preview
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(pal.corner))
                .background(pal.card)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(118.dp, 165.dp)
                    .clickable {
                        expression = (expression + 1) % 5
                        line = when (expression) {
                            1 -> "Yay! 😄"
                            2 -> "Whoa?! 😮"
                            3 -> "@_@ dizzy…"
                            4 -> "So sleepy… 😴"
                            else -> Phrases.pick(Mood.TAP, state.settings, cfg.personality, cfg.name)
                        }
                    }
            ) {
                AnimatedAvatar(cfg, Modifier.fillMaxSize(), expression, talking)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(10.dp)
                ) { Text(line, color = Color(0xFF1E1B2E), fontSize = 14.sp) }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cfg.name,
                    onValueChange = { set(cfg.copy(name = it.take(20))) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tap your buddy to try faces", color = pal.sub, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 10.dp,
            containerColor = Color.Transparent,
            contentColor = pal.onBg
        ) {
            categories.forEachIndexed { i, c ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(c, fontSize = 13.sp) })
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            when (tab) {
                0 -> SectionCard("Body") {
                    ChoiceChips("Personality (changes what I say!)", AvatarOptions.personalities, cfg.personality) {
                        set(cfg.copy(personality = it))
                        line = Phrases.pick(Mood.IDLE, state.settings, it, cfg.name)
                    }
                    ChoiceChips("Body shape", AvatarOptions.bodyShapes, cfg.bodyShape) { set(cfg.copy(bodyShape = it)) }
                    SliderRow("Body size", cfg.bodyScale, 0.75f..1.3f, label = { "${(it * 100).toInt()}%" }) { set(cfg.copy(bodyScale = it)) }
                    ColorRow("Skin", cfg.skinColor, AvatarOptions.skinTones) { set(cfg.copy(skinColor = it)) }
                    ColorRow("Pants", cfg.pantsColor) { set(cfg.copy(pantsColor = it)) }
                    ColorRow("Shoes", cfg.shoeColor) { set(cfg.copy(shoeColor = it)) }
                }
                1 -> SectionCard("Face") {
                    ChoiceChips("Head shape", AvatarOptions.headShapes, cfg.headShape) { set(cfg.copy(headShape = it)) }
                    SliderRow("Head size", cfg.headScale, 0.8f..1.3f, label = { "${(it * 100).toInt()}%" }) { set(cfg.copy(headScale = it)) }
                    ChoiceChips("Ears", AvatarOptions.ears, cfg.ears) { set(cfg.copy(ears = it)) }
                    ColorRow("Fur (animal ears & tails)", cfg.furColor, AvatarOptions.hairColors) { set(cfg.copy(furColor = it)) }
                    ChoiceChips("Nose", AvatarOptions.noses, cfg.nose) { set(cfg.copy(nose = it)) }
                    ChoiceChips("Cheeks", AvatarOptions.cheeks, cfg.cheeks) { set(cfg.copy(cheeks = it)) }
                    ColorRow("Cheek color", cfg.cheekColor, listOf(0xFFFF8A80, 0xFFF48FB1, 0xFFFFAB91, 0xFFE57373, 0xFFCE93D8, 0xFF80DEEA, 0xFFFFD54F)) { set(cfg.copy(cheekColor = it)) }
                    ChoiceChips("Facial hair", AvatarOptions.facialHair, cfg.facialHair) { set(cfg.copy(facialHair = it)) }
                }
                2 -> SectionCard("Eyes") {
                    ChoiceChips("Eye style", AvatarOptions.eyes, cfg.eyes) { set(cfg.copy(eyes = it)) }
                    ColorRow("Eye color", cfg.eyeColor, AvatarOptions.eyeColors) { set(cfg.copy(eyeColor = it)) }
                    SliderRow("Eye size", cfg.eyeSize, 0.6f..1.6f, label = { "${(it * 100).toInt()}%" }) { set(cfg.copy(eyeSize = it)) }
                    SliderRow("Eye spacing", cfg.eyeSpacing, 0.7f..1.35f, label = { "${(it * 100).toInt()}%" }) { set(cfg.copy(eyeSpacing = it)) }
                    ChoiceChips("Eyebrows", AvatarOptions.brows, cfg.brows) { set(cfg.copy(brows = it)) }
                }
                3 -> SectionCard("Mouth") {
                    ChoiceChips("Mouth", AvatarOptions.mouths, cfg.mouth) { set(cfg.copy(mouth = it)) }
                }
                4 -> SectionCard("Hair") {
                    ChoiceChips("Hairstyle", AvatarOptions.hair, cfg.hair) { set(cfg.copy(hair = it)) }
                    ColorRow("Hair color (also brows & beard)", cfg.hairColor, AvatarOptions.hairColors) { set(cfg.copy(hairColor = it)) }
                    SwitchRow("Shiny hair highlight", cfg.hairShine) { set(cfg.copy(hairShine = it)) }
                }
                5 -> SectionCard("Hats & Glasses") {
                    ChoiceChips("Headwear", AvatarOptions.headwear, cfg.headwear) { set(cfg.copy(headwear = it)) }
                    ColorRow("Headwear / hair-tie color", cfg.headwearColor) { set(cfg.copy(headwearColor = it)) }
                    ChoiceChips("Glasses", AvatarOptions.glasses, cfg.glasses) { set(cfg.copy(glasses = it)) }
                    ColorRow("Glasses color", cfg.glassesColor) { set(cfg.copy(glassesColor = it)) }
                }
                6 -> SectionCard("Outfit") {
                    ChoiceChips("Outfit", AvatarOptions.outfits, cfg.outfit) { set(cfg.copy(outfit = it)) }
                    ColorRow("Main color", cfg.outfitColor) { set(cfg.copy(outfitColor = it)) }
                    ColorRow("Second color (tie, stripes, cape…)", cfg.outfitColor2) { set(cfg.copy(outfitColor2 = it)) }
                    ChoiceChips("Pattern", AvatarOptions.patterns, cfg.pattern) { set(cfg.copy(pattern = it)) }
                }
                7 -> SectionCard("Extras") {
                    ChoiceChips("Holding", AvatarOptions.handItems, cfg.handItem) { set(cfg.copy(handItem = it)) }
                    ChoiceChips("Neck", AvatarOptions.necks, cfg.neck) { set(cfg.copy(neck = it)) }
                    ChoiceChips("On the back", AvatarOptions.backs, cfg.back) { set(cfg.copy(back = it)) }
                    ChoiceChips("Tail", AvatarOptions.tails, cfg.tail) { set(cfg.copy(tail = it)) }
                    ColorRow("Accessory color", cfg.accessoryColor) { set(cfg.copy(accessoryColor = it)) }
                }
                8 -> SectionCard("Drawing style") {
                    SliderRow("Outline thickness", cfg.outlineWidth, 0f..4f, label = { if (it < 0.1f) "none" else "%.1f".format(it) }) { set(cfg.copy(outlineWidth = it)) }
                    ColorRow("Outline color", cfg.outlineColor, listOf(0xFF2D2236, 0xFF000000, 0xFF3E2723, 0xFF1A237E, 0xFF4A148C, 0xFFFFFFFF, 0xFF607D8B)) { set(cfg.copy(outlineColor = it)) }
                    Text(
                        "Tip: long-press your buddy on the main screen to come back here.",
                        color = pal.sub, fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { Store.setAvatar(cfg); Store.update { it.copy(firstRun = false) }; onDone() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            ) { Text(if (state.firstRun) "Let's go, ${cfg.name.ifBlank { "Buddy" }}! 🚀" else "Done ✓", fontWeight = FontWeight.Bold) }
        }
    }
}
