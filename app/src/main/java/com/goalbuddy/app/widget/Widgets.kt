package com.goalbuddy.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.goalbuddy.app.MainActivity
import com.goalbuddy.app.avatar.AvatarPose
import com.goalbuddy.app.avatar.AvatarRenderer
import com.goalbuddy.app.avatar.Mood
import com.goalbuddy.app.avatar.Phrases
import com.goalbuddy.app.data.AppState
import com.goalbuddy.app.data.AvatarEvent
import com.goalbuddy.app.data.Events
import com.goalbuddy.app.data.Period
import com.goalbuddy.app.data.Periods
import com.goalbuddy.app.data.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

/** What the buddy currently says / does inside widgets. Tapping the buddy changes it. */
data class WidgetBuddy(val line: String = "", val expression: Int = 1, val onRight: Boolean = false, val armUp: Boolean = true)

object WidgetBuddyState {
    val flow = MutableStateFlow(WidgetBuddy())

    fun shuffle(s: AppState, mood: Mood = Mood.IDLE) {
        val p = Period.entries[s.settings.widgetPeriod.coerceIn(0, 3)]
        val goals = Store.goalsFor(s, p, Periods.currentKey(p))
        val line = Phrases.pick(
            mood, s.settings, s.avatar.personality, s.avatar.name,
            goals.filter { !it.done }.randomOrNull()?.title, goals.count { !it.done }, goals.count { it.done },
            p.label.lowercase()
        )
        val old = flow.value
        flow.value = WidgetBuddy(
            line = line,
            expression = listOf(0, 1, 1, 2, 0).random(),
            onRight = !old.onRight, // hops to the other edge of the widget every time
            armUp = Random.nextBoolean()
        )
    }
}

private fun avatarBitmap(s: AppState, b: WidgetBuddy, widthPx: Int): Bitmap =
    AvatarRenderer().bitmap(
        s.avatar,
        AvatarPose(expression = b.expression, armR = if (b.armUp) 150f else 14f, armL = 14f, shadow = true, lookX = if (b.onRight) -0.6f else 0.6f),
        widthPx
    )

private fun c(l: Long) = ColorProvider(Color(l))

private data class WColors(val bg: Color, val text: Color, val sub: Color, val accent: Color, val bubble: Color, val bubbleText: Color)

private fun colors(s: AppState): WColors {
    val st = s.settings
    return if (st.widgetDark) WColors(
        Color(0xFF17151F).copy(alpha = st.widgetOpacity), Color(0xFFF5F3FF), Color(0xFFB9B4CC), Color(st.accent),
        Color(0xFF2B2838), Color(0xFFF5F3FF)
    ) else WColors(
        Color(0xFFFFFFFF).copy(alpha = st.widgetOpacity), Color(0xFF1E1B2E), Color(0xFF6E6880), Color(st.accent),
        Color(0xFFF1ECFA), Color(0xFF1E1B2E)
    )
}

private fun openAppIntent(context: Context) = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun quickAddIntent(context: Context, period: Int) = Intent(context, QuickAddActivity::class.java)
    .putExtra(QuickAddActivity.EXTRA_PERIOD, period)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

// ===================================================================== Goals widget

class GoalWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        if (WidgetBuddyState.flow.value.line.isEmpty()) WidgetBuddyState.shuffle(Store.current, Mood.GREET)
        provideContent {
            val s by Store.state.collectAsState()
            val buddy by WidgetBuddyState.flow.collectAsState()
            GoalWidgetContent(context, s, buddy)
        }
    }
}

class GoalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalWidget()
}

private val GoalIdKey = ActionParameters.Key<String>("goalId")
private val PeriodKey = ActionParameters.Key<Int>("period")

@Composable
private fun GoalWidgetContent(context: Context, s: AppState, buddy: WidgetBuddy) {
    val st = s.settings
    val col = colors(s)
    val period = Period.entries[st.widgetPeriod.coerceIn(0, 3)]
    val key = Periods.currentKey(period)
    val all = Store.goalsFor(s, period, key)
    var goals = Store.arrange(all, st.copy(hideCompleted = false))
    if (!st.widgetShowDone || st.hideCompleted) goals = goals.filter { !it.done }
    val done = all.count { it.done }

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(col.bg)
            .cornerRadius(22.dp)
            .padding(10.dp)
    ) {
        // ---- header: buddy + bubble + add button. The buddy hops between left and right.
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (st.widgetShowAvatar && !buddy.onRight) {
                BuddyImage(s, buddy, 46)
                Spacer(GlanceModifier.width(6.dp))
            }
            Column(GlanceModifier.defaultWeight()) {
                if (st.widgetShowAvatar) {
                    Box(
                        GlanceModifier
                            .fillMaxWidth()
                            .background(col.bubble)
                            .cornerRadius(12.dp)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .clickable(actionRunCallback<PokeBuddyAction>())
                    ) {
                        Text(buddy.line, maxLines = 2, style = TextStyle(color = ColorProvider(col.bubbleText), fontSize = 12.sp))
                    }
                }
                Text(
                    "${period.emoji} ${Periods.title(period, 0)} · $done/${all.size} done",
                    style = TextStyle(color = ColorProvider(col.sub), fontSize = 11.sp),
                    modifier = GlanceModifier.padding(top = 3.dp, start = 2.dp)
                )
            }
            if (st.widgetShowAvatar && buddy.onRight) {
                Spacer(GlanceModifier.width(6.dp))
                BuddyImage(s, buddy, 46)
            }
            Spacer(GlanceModifier.width(6.dp))
            Box(
                GlanceModifier
                    .size(38.dp)
                    .background(col.accent)
                    .cornerRadius(19.dp)
                    .clickable(actionStartActivity(quickAddIntent(context, period.ordinal))),
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = TextStyle(color = c(0xFFFFFFFF), fontSize = 22.sp, fontWeight = FontWeight.Bold))
            }
        }

        // ---- period tabs
        Row(GlanceModifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Period.entries.forEach { p ->
                val sel = p == period
                Box(
                    GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 2.dp)
                        .background(if (sel) col.accent else col.bubble)
                        .cornerRadius(10.dp)
                        .padding(vertical = 4.dp)
                        .clickable(actionRunCallback<SetPeriodAction>(actionParametersOf(PeriodKey to p.ordinal))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        p.label,
                        style = TextStyle(
                            color = ColorProvider(if (sel) Color.White else col.text),
                            fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // ---- goal list with real checkboxes
        if (goals.isEmpty()) {
            Box(
                GlanceModifier.fillMaxWidth().defaultWeight().clickable(actionStartActivity(quickAddIntent(context, period.ordinal))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (all.isEmpty()) "No goals yet — tap + to plan ✨" else "All done! 🎉",
                    style = TextStyle(color = ColorProvider(col.sub), fontSize = 13.sp)
                )
            }
        } else {
            LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(goals, itemId = { it.id.hashCode().toLong() }) { g ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                        CheckBox(
                            checked = g.done,
                            onCheckedChange = actionRunCallback<ToggleGoalAction>(actionParametersOf(GoalIdKey to g.id)),
                            text = (if (g.emoji.isNotEmpty()) g.emoji + " " else "") + g.title +
                                (if (g.priority == 2) " 🔥" else if (g.priority == 1) " ⭐" else ""),
                            style = TextStyle(
                                color = ColorProvider(if (g.done) col.sub else col.text),
                                fontSize = 14.sp,
                                textDecoration = if (g.done && st.strikeCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            maxLines = 2,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }

        // ---- footer
        Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Open Goal Buddy ›",
                style = TextStyle(color = ColorProvider(col.accent), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(actionStartActivity(openAppIntent(context)))
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                "✎ Plan",
                style = TextStyle(color = ColorProvider(col.sub), fontSize = 12.sp),
                modifier = GlanceModifier.clickable(actionStartActivity(quickAddIntent(context, period.ordinal)))
            )
        }
    }
}

@Composable
private fun BuddyImage(s: AppState, buddy: WidgetBuddy, widthDp: Int) {
    Image(
        provider = ImageProvider(avatarBitmap(s, buddy, 180)),
        contentDescription = s.avatar.name,
        modifier = GlanceModifier
            .size(widthDp.dp, (widthDp * AvatarRenderer.BOX_RATIO).dp)
            .clickable(actionRunCallback<PokeBuddyAction>())
    )
}

class ToggleGoalAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Store.init(context)
        val id = parameters[GoalIdKey] ?: return
        val goal = Store.current.goals.firstOrNull { it.id == id } ?: return
        val nowDone = Store.toggle(id)
        if (nowDone) {
            val left = Store.goalsFor(Store.current, goal.period, goal.key).count { !it.done }
            WidgetBuddyState.shuffle(Store.current, if (left == 0) Mood.ALL_DONE else Mood.GOAL_DONE)
            Events.avatar.tryEmit(AvatarEvent.GoalDone(goal.title, left == 0))
        }
        Store.flush()
        GoalWidget().updateAll(context)
        BuddyWidget().updateAll(context)
    }
}

class SetPeriodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Store.init(context)
        val p = parameters[PeriodKey] ?: return
        Store.setSettings(Store.current.settings.copy(widgetPeriod = p))
        WidgetBuddyState.shuffle(Store.current, Mood.PAGE)
        Store.flush()
        GoalWidget().updateAll(context)
    }
}

class PokeBuddyAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Store.init(context)
        WidgetBuddyState.shuffle(Store.current, if (Random.nextBoolean()) Mood.TAP else Mood.IDLE)
        GoalWidget().updateAll(context)
        BuddyWidget().updateAll(context)
    }
}

// ===================================================================== Buddy (avatar) widget

class BuddyWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Store.init(context)
        if (WidgetBuddyState.flow.value.line.isEmpty()) WidgetBuddyState.shuffle(Store.current, Mood.GREET)
        provideContent {
            val s by Store.state.collectAsState()
            val buddy by WidgetBuddyState.flow.collectAsState()
            BuddyWidgetContent(context, s, buddy)
        }
    }
}

class BuddyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BuddyWidget()
}

@Composable
private fun BuddyWidgetContent(context: Context, s: AppState, buddy: WidgetBuddy) {
    val col = colors(s)
    val key = Periods.currentKey(Period.DAY)
    val today = Store.goalsFor(s, Period.DAY, key)
    val done = today.count { it.done }
    val next = today.firstOrNull { !it.done }

    Row(
        GlanceModifier
            .fillMaxSize()
            .background(col.bg)
            .cornerRadius(22.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!buddy.onRight) BuddyImage(s, buddy, 64)
        Column(GlanceModifier.defaultWeight().padding(horizontal = 6.dp)) {
            Box(
                GlanceModifier
                    .fillMaxWidth()
                    .background(col.bubble)
                    .cornerRadius(14.dp)
                    .padding(8.dp)
                    .clickable(actionRunCallback<PokeBuddyAction>())
            ) {
                Text(buddy.line, maxLines = 3, style = TextStyle(color = ColorProvider(col.bubbleText), fontSize = 13.sp))
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(
                "${s.avatar.name} · today $done/${today.size} ✓",
                style = TextStyle(color = ColorProvider(col.text), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            if (next != null) {
                CheckBox(
                    checked = false,
                    onCheckedChange = actionRunCallback<ToggleGoalAction>(actionParametersOf(GoalIdKey to next.id)),
                    text = "Next: ${next.title}",
                    style = TextStyle(color = ColorProvider(col.sub), fontSize = 12.sp),
                    maxLines = 1
                )
            }
            Text(
                "+ Add goal",
                style = TextStyle(color = ColorProvider(col.accent), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.padding(top = 2.dp).clickable(actionStartActivity(quickAddIntent(context, 0)))
            )
        }
        if (buddy.onRight) BuddyImage(s, buddy, 64)
    }
}
