package com.goalbuddy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.goalbuddy.app.data.AvatarEvent
import com.goalbuddy.app.data.Events
import com.goalbuddy.app.data.Period
import com.goalbuddy.app.data.Periods
import com.goalbuddy.app.data.Store
import com.goalbuddy.app.ui.AvatarEditorScreen
import com.goalbuddy.app.ui.AvatarOverlay
import com.goalbuddy.app.ui.BuddyBrain
import com.goalbuddy.app.ui.GoalTheme
import com.goalbuddy.app.ui.HomeScreen
import com.goalbuddy.app.ui.LocalPalette
import com.goalbuddy.app.ui.PageOffsets
import com.goalbuddy.app.ui.ParticleLayer
import com.goalbuddy.app.ui.ParticleSettingsScreen
import com.goalbuddy.app.ui.PhraseContext
import com.goalbuddy.app.ui.RootOrigin
import com.goalbuddy.app.ui.SettingsScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        setContent { GoalBuddyRoot() }
    }

    override fun onResume() {
        super.onResume()
        Store.init(this) // also handles a new day starting while the app was in the background
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoalBuddyRoot() {
    val state by Store.state.collectAsState()
    var screen by rememberSaveable { mutableStateOf(if (state.firstRun) "avatar" else "home") }
    val pagerState = rememberPagerState(initialPage = state.settings.startPage.coerceIn(0, 3)) { 4 }
    val brain = remember { BuddyBrain() }

    BackHandler(enabled = screen != "home") { screen = "home" }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().drop(1).collect {
            Events.avatar.tryEmit(AvatarEvent.PageChanged(Period.entries[it]))
        }
    }

    GoalTheme(state.settings) {
        val pal = LocalPalette.current
        val st = state.settings
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(pal.bg1, pal.bg2)))
        ) {
            BackgroundPattern(st.bgStyle, pal.onBg)
            Box(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .onGloballyPositioned { RootOrigin.offset = it.positionInRoot() }
                    .nestedScroll(brain.scrollConnection)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                if (e.type == PointerEventType.Press) {
                                    val p = e.changes.firstOrNull()?.position ?: continue
                                    brain.onGlobalTouch(p)
                                    Events.touch.tryEmit(p)
                                }
                            }
                        }
                    }
            ) {
                if (!state.particles.inFront) ParticleLayer(state.particles, pal.accent)

                when (screen) {
                    "avatar" -> AvatarEditorScreen(state) { screen = "home" }
                    "settings" -> SettingsScreen(state, onBack = { screen = "home" }, onParticles = { screen = "particles" })
                    "particles" -> ParticleSettingsScreen(state) { screen = "settings" }
                    else -> HomeScreen(
                        state, pagerState,
                        onAvatar = { screen = "avatar" },
                        onParticles = { screen = "particles" },
                        onSettings = { screen = "settings" }
                    )
                }

                if (state.particles.inFront) ParticleLayer(state.particles, pal.accent)

                if (st.avatarEnabled && screen != "avatar") {
                    AvatarOverlay(
                        brain = brain,
                        state = state,
                        phraseContext = {
                            val s = Store.current
                            val period = Period.entries[pagerState.currentPage]
                            val key = Periods.keyFor(period, PageOffsets.values[period.ordinal])
                            val goals = Store.goalsFor(s, period, key)
                            PhraseContext(
                                goal = goals.filter { !it.done }.randomOrNull()?.title,
                                left = goals.count { !it.done },
                                done = goals.count { it.done },
                                page = period.label.lowercase()
                            )
                        },
                        onOpenEditor = { screen = "avatar" }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundPattern(style: Int, ink: androidx.compose.ui.graphics.Color) {
    if (style < 2) return
    Canvas(Modifier.fillMaxSize()) {
        val step = 26.dp.toPx()
        val c = ink.copy(alpha = 0.06f)
        var y = 0f
        var row = 0
        while (y < size.height) {
            if (style == 2) {
                var x = if (row % 2 == 0) 0f else step / 2f
                while (x < size.width) { drawCircle(c, 2.2.dp.toPx(), Offset(x, y)); x += step }
            } else {
                drawLine(c, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            y += step; row++
        }
        if (style == 3) {
            var x = 0f
            while (x < size.width) { drawLine(c, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += step }
        }
    }
}
