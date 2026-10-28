package com.goalbuddy.app.data

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.MutableSharedFlow

/** Little in-app event bus between the goal list, the avatar and the particles. */
object Events {
    /** A confetti burst; position in root (window) pixels, or null for "from the top". */
    val burst = MutableSharedFlow<Offset?>(extraBufferCapacity = 8)

    /** A short sparkle where the user touched. */
    val touch = MutableSharedFlow<Offset>(extraBufferCapacity = 16)

    /** Ask the avatar to react. */
    val avatar = MutableSharedFlow<AvatarEvent>(extraBufferCapacity = 8)
}

sealed class AvatarEvent {
    data class GoalDone(val title: String, val allDone: Boolean) : AvatarEvent()
    data class GoalAdded(val title: String) : AvatarEvent()
    data object GoalDeleted : AvatarEvent()
    data class PageChanged(val period: Period) : AvatarEvent()
    data class Say(val text: String) : AvatarEvent()
}
