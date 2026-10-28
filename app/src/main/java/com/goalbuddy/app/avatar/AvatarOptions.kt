package com.goalbuddy.app.avatar

import com.goalbuddy.app.data.AvatarConfig
import kotlin.random.Random

/** Names shown in the avatar editor. The index of each name is what gets saved. */
object AvatarOptions {
    val personalities = listOf("😊 Cheerful", "💅 Sassy", "🌊 Chill", "🔥 Hype", "🤓 Nerdy", "😾 Grumpy-cute", "👑 Royal", "🏴‍☠️ Pirate")
    val headShapes = listOf("Round", "Oval", "Wide", "Square", "Egg", "Pointy chin")
    val bodyShapes = listOf("Round", "Bean", "Tall", "Square", "Pear", "Tiny")
    val ears = listOf("None", "Human", "Elf", "Cat", "Bear", "Bunny", "Floppy dog", "Mouse")
    val eyes = listOf("Round", "Sparkly", "Sleepy", "Happy ^^", "Dots", "Anime", "Cool", "Cyclops", "Stars", "Hearts")
    val brows = listOf("None", "Thin", "Thick", "Angry", "Worried", "Unibrow", "Fluffy")
    val noses = listOf("None", "Dot", "Button", "Pointy", "Big round", "Piggy", "Clown")
    val mouths = listOf("Smile", "Big grin", "Open", "Cat :3", "Smirk", "Teeth", "Tongue out", "Small o", "Flat", "Fangs", "Buck teeth")
    val cheeks = listOf("None", "Blush", "Freckles", "Blush + freckles", "Stripes", "Hearts", "Swirls")
    val facialHair = listOf("None", "Mustache", "Handlebar", "Beard", "Goatee", "Stubble")
    val hair = listOf("Bald", "Short", "Spiky", "Bowl", "Long", "Ponytail", "Pigtails", "Afro", "Mohawk", "Bun", "Curly", "Side swept", "Bob")
    val headwear = listOf("None", "Cap", "Beanie", "Crown", "Top hat", "Wizard hat", "Headband", "Bow", "Party hat", "Horns", "Halo", "Antennae", "Flower")
    val glasses = listOf("None", "Round", "Square", "Sunglasses", "Monocle", "Star", "Heart", "Goggles", "Eyepatch")
    val outfits = listOf("T-shirt", "Hoodie", "Suit", "Dress", "Overalls", "Striped sweater", "Superhero", "Robe", "Tank top", "Jersey")
    val patterns = listOf("None", "Stripes", "Dots", "Star", "Heart", "Zigzag")
    val necks = listOf("None", "Scarf", "Bow tie", "Necklace", "Medal", "Bell collar")
    val handItems = listOf("None", "Star wand", "Balloon", "Flower", "Coffee", "Sword", "Book", "Flag", "Ice cream", "Trophy")
    val backs = listOf("None", "Angel wings", "Bat wings", "Backpack", "Cape", "Fairy wings", "Jetpack")
    val tails = listOf("None", "Cat", "Fox", "Devil", "Bunny puff")

    val skinTones: List<Long> = listOf(
        0xFFFFE0C7, 0xFFFFD7B5, 0xFFF1C27D, 0xFFE0AC69, 0xFFC68642, 0xFF8D5524, 0xFF5C3A1E,
        // fantasy
        0xFFA5D6A7, 0xFF81D4FA, 0xFFCE93D8, 0xFFFFAB91, 0xFFFFF59D, 0xFFB0BEC5, 0xFFF8BBD0
    )
    val hairColors: List<Long> = listOf(
        0xFF1B1B1B, 0xFF3E2723, 0xFF5D3A1A, 0xFF8D6E63, 0xFFC0803A, 0xFFF4D27A, 0xFFE0E0E0, 0xFFB7472A,
        0xFFFF4081, 0xFF7C4DFF, 0xFF00B0FF, 0xFF00E676, 0xFFFF9100, 0xFF651FFF
    )
    val eyeColors: List<Long> = listOf(
        0xFF3B7DD8, 0xFF2E7D32, 0xFF6D4C41, 0xFF212121, 0xFF8E24AA, 0xFFE53935, 0xFFFFB300, 0xFF00ACC1, 0xFFEC407A
    )
    val vivid: List<Long> = listOf(
        0xFF7C4DFF, 0xFF536DFE, 0xFF2196F3, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
        0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF795548,
        0xFF607D8B, 0xFF263238, 0xFFFFFFFF, 0xFFF5F5F5, 0xFF212121, 0xFFFFCDD2, 0xFFC8E6C9, 0xFFBBDEFB
    )

    private fun <T> List<T>.r(): T = this[Random.nextInt(size)]
    private fun idx(list: List<String>, noneChance: Float = 0f): Int =
        if (Random.nextFloat() < noneChance) 0 else Random.nextInt(list.size)

    /** A brand-new random character (keeps the name). */
    fun random(name: String): AvatarConfig {
        val fantasy = Random.nextFloat() < 0.25f
        return AvatarConfig(
            name = name,
            personality = Random.nextInt(personalities.size),
            headScale = 0.9f + Random.nextFloat() * 0.25f,
            bodyScale = 0.85f + Random.nextFloat() * 0.3f,
            eyeSize = 0.85f + Random.nextFloat() * 0.4f,
            eyeSpacing = 0.85f + Random.nextFloat() * 0.3f,
            headShape = idx(headShapes),
            bodyShape = idx(bodyShapes),
            skinColor = if (fantasy) skinTones.subList(7, skinTones.size).r() else skinTones.subList(0, 7).r(),
            ears = if (Random.nextFloat() < 0.5f) 1 else idx(ears),
            furColor = hairColors.r(),
            eyes = idx(eyes),
            eyeColor = eyeColors.r(),
            brows = idx(brows, 0.15f),
            nose = idx(noses, 0.1f),
            mouth = idx(mouths),
            cheeks = idx(cheeks, 0.3f),
            cheekColor = listOf(0xFFFF8A80, 0xFFF48FB1, 0xFFFFAB91, 0xFFE57373).r(),
            facialHair = if (Random.nextFloat() < 0.75f) 0 else idx(facialHair),
            hair = idx(hair),
            hairColor = hairColors.r(),
            hairShine = Random.nextBoolean(),
            headwear = idx(headwear, 0.45f),
            headwearColor = vivid.r(),
            glasses = idx(glasses, 0.6f),
            glassesColor = listOf(0xFF263238, 0xFF5D4037, 0xFFE91E63, 0xFFFFC107, 0xFF2196F3).r(),
            outfit = idx(outfits),
            outfitColor = vivid.r(),
            outfitColor2 = vivid.r(),
            pattern = idx(patterns, 0.5f),
            pantsColor = listOf(0xFF37474F, 0xFF1565C0, 0xFF5D4037, 0xFF212121, 0xFF6A1B9A, 0xFF2E7D32).r(),
            shoeColor = vivid.r(),
            neck = idx(necks, 0.6f),
            handItem = idx(handItems, 0.5f),
            back = idx(backs, 0.6f),
            tail = idx(tails, 0.7f),
            accessoryColor = vivid.r()
        )
    }
}
