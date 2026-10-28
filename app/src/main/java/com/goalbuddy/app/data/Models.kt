package com.goalbuddy.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Period(val label: String, val emoji: String) {
    DAY("Day", "☀️"),
    WEEK("Week", "📅"),
    MONTH("Month", "🗓️"),
    YEAR("Year", "🏔️")
}

@Serializable
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val note: String = "",
    val period: Period = Period.DAY,
    /** Which day/week/month/year this goal belongs to, e.g. 2026-10-27, 2026-W44, 2026-10, 2026 */
    val key: String = "",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val doneAt: Long = 0L,
    /** 0 = normal, 1 = important, 2 = urgent */
    val priority: Int = 0,
    val emoji: String = "",
    /** Repeat this goal every new day / week / month / year */
    val repeating: Boolean = false,
    /** Goals created from the same repeating goal share a seriesId */
    val seriesId: String = "",
    /** How many times this goal was carried over from an earlier period */
    val carried: Int = 0
)

/**
 * Every part of the avatar. Integers are indexes into the option lists in AvatarOptions,
 * colors are ARGB longs (0xAARRGGBB).
 */
@Serializable
data class AvatarConfig(
    val name: String = "Buddy",
    val personality: Int = 0,
    // proportions
    val headScale: Float = 1f,
    val bodyScale: Float = 1f,
    val eyeSize: Float = 1f,
    val eyeSpacing: Float = 1f,
    val outlineWidth: Float = 2.2f,
    val outlineColor: Long = 0xFF2D2236,
    // head & body
    val headShape: Int = 0,
    val bodyShape: Int = 1,
    val skinColor: Long = 0xFFFFD7B5,
    val ears: Int = 1,
    val furColor: Long = 0xFF8D6E63,
    // face
    val eyes: Int = 0,
    val eyeColor: Long = 0xFF3B7DD8,
    val brows: Int = 1,
    val nose: Int = 1,
    val mouth: Int = 0,
    val cheeks: Int = 1,
    val cheekColor: Long = 0xFFFF8A80,
    val facialHair: Int = 0,
    // hair
    val hair: Int = 1,
    val hairColor: Long = 0xFF5D3A1A,
    val hairShine: Boolean = true,
    // headwear / glasses
    val headwear: Int = 0,
    val headwearColor: Long = 0xFFFF7043,
    val glasses: Int = 0,
    val glassesColor: Long = 0xFF263238,
    // clothes
    val outfit: Int = 0,
    val outfitColor: Long = 0xFF7C4DFF,
    val outfitColor2: Long = 0xFFFFC107,
    val pattern: Int = 0,
    val pantsColor: Long = 0xFF37474F,
    val shoeColor: Long = 0xFFE53935,
    // extras
    val neck: Int = 0,
    val handItem: Int = 0,
    val back: Int = 0,
    val tail: Int = 0,
    val accessoryColor: Long = 0xFFFFD54F
)

@Serializable
data class ParticleConfig(
    val enabled: Boolean = true,
    /** see ParticleOptions.shapes */
    val shape: Int = 1,
    /** see ParticleOptions.motions */
    val motion: Int = 0,
    val count: Int = 26,
    val speed: Float = 1f,
    val sizeMin: Float = 3f,
    val sizeMax: Float = 8f,
    /** 0 accent, 1 rainbow, 2 pastel, 3 custom, 4 white & gold */
    val colorMode: Int = 2,
    val customColors: List<Long> = listOf(0xFFFF80AB, 0xFF80D8FF, 0xFFFFE57F),
    val opacity: Float = 0.6f,
    val fadeInOut: Boolean = true,
    val lifetime: Float = 6f,
    val spin: Boolean = true,
    val wind: Float = 0f,
    val inFront: Boolean = false,
    val burstOnDone: Boolean = true,
    val burstCount: Int = 40,
    val tapSparkles: Boolean = true,
    val glow: Boolean = false
)

@Serializable
data class AppSettings(
    // look
    val themePreset: Int = 0,
    val accent: Long = 0xFF7C4DFF,
    val bgColor1: Long = 0xFFF3EEFF,
    val bgColor2: Long = 0xFFFFE9F3,
    /** 0 solid, 1 gradient, 2 gradient + dots, 3 gradient + grid */
    val bgStyle: Int = 2,
    val cardColor: Long = 0xFFFFFFFF,
    val cardOpacity: Float = 0.85f,
    val cornerRadius: Int = 18,
    val fontScale: Float = 1f,
    val compact: Boolean = false,
    val tabEmoji: Boolean = true,
    val startPage: Int = 0,
    // list behaviour
    val hideCompleted: Boolean = false,
    val hideDelaySec: Int = 2,
    val completedToBottom: Boolean = true,
    val strikeCompleted: Boolean = true,
    val dimCompleted: Boolean = true,
    /** 0 oldest first, 1 newest first, 2 priority, 3 A–Z */
    val sortMode: Int = 2,
    /** 0 square, 1 circle, 2 emoji, 3 star, 4 heart */
    val checkboxStyle: Int = 1,
    val showProgress: Boolean = true,
    val showCounts: Boolean = true,
    val showNotes: Boolean = true,
    val showPeriodNav: Boolean = true,
    val carryOver: Boolean = true,
    val confirmDelete: Boolean = true,
    val celebrate: Boolean = true,
    val haptics: Boolean = true,
    // avatar behaviour
    val avatarEnabled: Boolean = true,
    val avatarSize: Int = 86,
    val avatarSpeed: Float = 1f,
    /** 0 rarely, 1 normal, 2 chatty, 3 non-stop */
    val chatFrequency: Int = 1,
    val bubbleSeconds: Float = 3.5f,
    /** 0 auto, 1 left, 2 right */
    val bubbleSide: Int = 0,
    val edgeBottom: Boolean = true,
    val edgeTop: Boolean = true,
    val edgeLeft: Boolean = true,
    val edgeRight: Boolean = true,
    val reactTaps: Boolean = true,
    val reactScroll: Boolean = true,
    val eyesFollowTouch: Boolean = true,
    val phraseFunny: Boolean = true,
    val phraseMotivation: Boolean = true,
    val phraseGoals: Boolean = true,
    val phraseFacts: Boolean = true,
    // widget
    val widgetPeriod: Int = 0,
    val widgetShowDone: Boolean = true,
    val widgetShowAvatar: Boolean = true,
    val widgetOpacity: Float = 0.92f,
    val widgetDark: Boolean = false,
    // lock screen
    val lockNotif: Boolean = false,
    val lockPeriod: Int = 0,
    val lockShowDone: Boolean = true,
    val lockAvatarLine: Boolean = true
)

@Serializable
data class AppState(
    val goals: List<Goal> = emptyList(),
    val avatar: AvatarConfig = AvatarConfig(),
    val settings: AppSettings = AppSettings(),
    val particles: ParticleConfig = ParticleConfig(),
    /** period name -> last period key that was processed for repeat / carry-over */
    val lastKeys: Map<String, String> = emptyMap(),
    val firstRun: Boolean = true
)
