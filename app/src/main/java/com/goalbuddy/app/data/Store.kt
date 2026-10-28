package com.goalbuddy.app.data

import android.content.Context
import com.goalbuddy.app.notify.LockScreen
import com.goalbuddy.app.widget.BuddyWidget
import com.goalbuddy.app.widget.GoalWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Single source of truth for the whole app. The app screens, the widgets, the quick-add
 * window and the lock-screen card all read and write through here.
 */
object Store {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val saveMutex = Mutex()
    private var appContext: Context? = null
    private var file: File? = null
    private var refreshJob: Job? = null

    @Synchronized
    fun init(context: Context) {
        if (appContext != null) {
            rollover()
            return
        }
        val ctx = context.applicationContext
        appContext = ctx
        val f = File(ctx.filesDir, "goalbuddy_state.json")
        file = f
        val loaded = runCatching {
            if (f.exists()) json.decodeFromString(AppState.serializer(), f.readText()) else null
        }.getOrNull()
        _state.value = loaded ?: starterState()
        rollover()
    }

    val current: AppState get() = _state.value

    fun update(transform: (AppState) -> AppState) {
        _state.update(transform)
        persist()
    }

    private var saveJob: Job? = null

    private fun persist() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300) // collapse rapid changes (sliders) into one write
            flush()
        }
        refreshSurfaces()
    }

    /** Write the current state to disk right now. */
    suspend fun flush() {
        val f = file ?: return
        val snapshot = _state.value
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            saveMutex.withLock {
                runCatching {
                    val tmp = File(f.parentFile, f.name + ".tmp")
                    tmp.writeText(json.encodeToString(AppState.serializer(), snapshot))
                    if (!tmp.renameTo(f)) {
                        f.writeText(tmp.readText()); tmp.delete()
                    }
                }
            }
        }
    }

    /** Refresh widgets + lock screen card (debounced). */
    fun refreshSurfaces() {
        val ctx = appContext ?: return
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(250)
            runCatching { GoalWidget().updateAll(ctx) }
            runCatching { BuddyWidget().updateAll(ctx) }
            runCatching { LockScreen.update(ctx, _state.value) }
        }
    }

    // ---------------------------------------------------------------- goals

    fun addGoal(
        title: String, period: Period, key: String,
        priority: Int = 0, emoji: String = "", repeating: Boolean = false, note: String = ""
    ) {
        val t = title.trim()
        if (t.isEmpty()) return
        val id = UUID.randomUUID().toString()
        update {
            it.copy(
                goals = it.goals + Goal(
                    id = id, title = t, note = note.trim(), period = period, key = key,
                    priority = priority, emoji = emoji, repeating = repeating, seriesId = id
                )
            )
        }
    }

    fun toggle(id: String): Boolean {
        var nowDone = false
        update { s ->
            s.copy(goals = s.goals.map { g ->
                if (g.id == id) {
                    nowDone = !g.done
                    g.copy(done = nowDone, doneAt = if (nowDone) System.currentTimeMillis() else 0L)
                } else g
            })
        }
        return nowDone
    }

    fun saveGoal(goal: Goal) = update { s -> s.copy(goals = s.goals.map { if (it.id == goal.id) goal else it }) }

    fun delete(id: String) = update { s -> s.copy(goals = s.goals.filterNot { it.id == id }) }

    fun clearDone(period: Period, key: String) =
        update { s -> s.copy(goals = s.goals.filterNot { it.period == period && it.key == key && it.done }) }

    fun uncheckAll(period: Period, key: String) = update { s ->
        s.copy(goals = s.goals.map { if (it.period == period && it.key == key) it.copy(done = false, doneAt = 0) else it })
    }

    fun moveGoal(id: String, newKey: String) =
        update { s -> s.copy(goals = s.goals.map { if (it.id == id) it.copy(key = newKey) else it }) }

    fun goalsFor(s: AppState, period: Period, key: String): List<Goal> =
        s.goals.filter { it.period == period && it.key == key }

    /** Apply the user's sort + "completed" rules. */
    fun arrange(list: List<Goal>, st: AppSettings, now: Long = System.currentTimeMillis()): List<Goal> {
        var l = when (st.sortMode) {
            1 -> list.sortedByDescending { it.createdAt }
            2 -> list.sortedWith(compareByDescending<Goal> { it.priority }.thenBy { it.createdAt })
            3 -> list.sortedBy { it.title.lowercase() }
            else -> list.sortedBy { it.createdAt }
        }
        if (st.completedToBottom) l = l.sortedBy { it.done } // stable sort keeps order inside groups
        if (st.hideCompleted) l = l.filter { !it.done || now - it.doneAt < st.hideDelaySec * 1000L }
        return l
    }

    // ---------------------------------------------------------------- settings

    fun setAvatar(a: AvatarConfig) = update { it.copy(avatar = a) }
    fun setSettings(st: AppSettings) = update { it.copy(settings = st) }
    fun setParticles(p: ParticleConfig) = update { it.copy(particles = p) }

    // ---------------------------------------------------------------- rollover

    /** When a new day/week/month/year starts: re-create repeating goals and carry unfinished ones. */
    @Synchronized
    fun rollover() {
        val s = _state.value
        var goals = s.goals
        val lastKeys = s.lastKeys.toMutableMap()
        var changed = false
        for (p in Period.entries) {
            val cur = Periods.currentKey(p)
            if (lastKeys[p.name] == cur) continue
            changed = true
            lastKeys[p.name] = cur
            // carry over unfinished, non-repeating goals from the past
            if (s.settings.carryOver) {
                goals = goals.map { g ->
                    if (g.period == p && !g.done && !g.repeating && g.key < cur) g.copy(key = cur, carried = g.carried + 1) else g
                }
            }
            // repeating goals: one fresh copy per series in the new period
            val series = goals.filter { it.period == p && it.repeating }.groupBy { it.seriesId.ifEmpty { it.id } }
            val additions = mutableListOf<Goal>()
            for ((sid, members) in series) {
                if (members.any { it.key == cur }) continue
                val latest = members.filter { it.key < cur }.maxByOrNull { it.key } ?: continue
                additions += latest.copy(
                    id = UUID.randomUUID().toString(), key = cur, done = false, doneAt = 0,
                    createdAt = System.currentTimeMillis(), seriesId = sid, carried = 0
                )
            }
            goals = goals + additions
        }
        if (changed) {
            _state.value = s.copy(goals = goals, lastKeys = lastKeys)
            persist()
        }
    }

    private fun starterState(): AppState {
        val d = Periods.currentKey(Period.DAY)
        val w = Periods.currentKey(Period.WEEK)
        val m = Periods.currentKey(Period.MONTH)
        val y = Periods.currentKey(Period.YEAR)
        fun g(t: String, p: Period, k: String, e: String = "", pr: Int = 0, rep: Boolean = false): Goal {
            val id = UUID.randomUUID().toString()
            return Goal(id = id, title = t, period = p, key = k, emoji = e, priority = pr, repeating = rep, seriesId = id)
        }
        return AppState(
            goals = listOf(
                g("Drink 8 glasses of water", Period.DAY, d, "💧", rep = true),
                g("Make my Goal Buddy avatar", Period.DAY, d, "🎨", pr = 1),
                g("Try the home screen widget", Period.DAY, d, "📱"),
                g("Exercise 3 times", Period.WEEK, w, "🏃"),
                g("Read one book", Period.MONTH, m, "📚"),
                g("Learn a new skill", Period.YEAR, y, "🚀", pr = 2)
            )
        )
    }

    fun resetGoals() = update { it.copy(goals = emptyList()) }
    fun resetSettings() = update { it.copy(settings = AppSettings(), particles = ParticleConfig()) }
}
