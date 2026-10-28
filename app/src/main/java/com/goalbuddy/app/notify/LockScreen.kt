package com.goalbuddy.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.goalbuddy.app.MainActivity
import com.goalbuddy.app.R
import com.goalbuddy.app.avatar.AvatarPose
import com.goalbuddy.app.avatar.AvatarRenderer
import com.goalbuddy.app.avatar.Mood
import com.goalbuddy.app.avatar.Phrases
import com.goalbuddy.app.data.AppState
import com.goalbuddy.app.data.Period
import com.goalbuddy.app.data.Periods
import com.goalbuddy.app.data.Store
import com.goalbuddy.app.widget.QuickAddActivity
import kotlinx.coroutines.launch

/**
 * A live "goal card" pinned in the notification shade and shown on the lock screen,
 * with ✓ Done (ticks the next goal) and + Add (opens the planning window over the lock screen).
 */
object LockScreen {
    private const val CHANNEL = "goal_lock_card"
    private const val ID = 4207
    const val ACTION_DONE_NEXT = "com.goalbuddy.app.DONE_NEXT"
    private var lastLine = ""
    private var lastLineAt = 0L

    @SuppressLint("MissingPermission")
    fun update(context: Context, s: AppState) {
        val nm = NotificationManagerCompat.from(context)
        val st = s.settings
        if (!st.lockNotif) { nm.cancel(ID); return }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Lock screen goals", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Your goals on the lock screen"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }

        val period = Period.entries[st.lockPeriod.coerceIn(0, 3)]
        val goals = Store.arrange(Store.goalsFor(s, period, Periods.currentKey(period)), st.copy(hideCompleted = false))
        val shown = if (st.lockShowDone) goals else goals.filter { !it.done }
        val done = goals.count { it.done }
        val next = goals.firstOrNull { !it.done }

        // keep the same buddy line for a while so the card doesn't change constantly
        val now = System.currentTimeMillis()
        if (lastLine.isEmpty() || now - lastLineAt > 20 * 60_000L) {
            lastLine = Phrases.pick(
                Mood.IDLE, st, s.avatar.personality, s.avatar.name, next?.title,
                goals.count { !it.done }, done, period.label.lowercase()
            )
            lastLineAt = now
        }

        val inbox = NotificationCompat.InboxStyle()
        shown.take(7).forEach { g -> inbox.addLine((if (g.done) "☑ " else "☐ ") + g.title) }
        if (shown.size > 7) inbox.setSummaryText("+${shown.size - 7} more")
        if (goals.isEmpty()) inbox.addLine("No goals yet — tap + Add")
        if (st.lockAvatarLine) inbox.setBigContentTitle("${s.avatar.name}: $lastLine")

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openApp = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags
        )
        val addIntent = PendingIntent.getActivity(
            context, 2,
            Intent(context, QuickAddActivity::class.java)
                .putExtra(QuickAddActivity.EXTRA_PERIOD, period.ordinal)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            flags
        )
        val doneIntent = PendingIntent.getBroadcast(
            context, 3, Intent(context, NotifActionReceiver::class.java).setAction(ACTION_DONE_NEXT), flags
        )

        val avatar = runCatching { AvatarRenderer().bitmap(s.avatar, AvatarPose(expression = 1, armR = 150f), 160) }.getOrNull()

        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_goal)
            .setContentTitle("${period.emoji} ${Periods.title(period, 0)}: $done/${goals.size} done")
            .setContentText(next?.let { "Next: ${it.title}" } ?: if (goals.isEmpty()) "Plan your goals" else "All done! 🎉")
            .setStyle(inbox)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(st.accent.toInt())
            .setContentIntent(openApp)
            .setProgress(goals.size.coerceAtLeast(1), done, false)
        if (avatar != null) b.setLargeIcon(avatar)
        if (next != null) b.addAction(0, "✓ Done: ${next.title.take(18)}", doneIntent)
        b.addAction(0, "+ Add", addIntent)
        nm.notify(ID, b.build())
    }
}

class NotifActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LockScreen.ACTION_DONE_NEXT) return
        val pending = goAsync()
        Store.init(context)
        val s = Store.current
        val period = Period.entries[s.settings.lockPeriod.coerceIn(0, 3)]
        val next = Store.arrange(Store.goalsFor(s, period, Periods.currentKey(period)), s.settings.copy(hideCompleted = false))
            .firstOrNull { !it.done }
        if (next != null) Store.toggle(next.id)
        Store.scope.launch {
            try {
                Store.flush()
                LockScreen.update(context, Store.current)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        Store.refreshSurfaces()
    }
}
