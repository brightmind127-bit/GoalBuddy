package com.goalbuddy.app.avatar

import com.goalbuddy.app.data.AppSettings
import java.time.LocalTime
import kotlin.random.Random

/**
 * The speech-bubble phrase bank. Placeholders:
 *  {name} avatar name · {goal} a random unfinished goal · {left} goals left · {done} goals done
 */
enum class Mood { TAP, POKE_SPAM, SCROLL, PAGE, GOAL_DONE, ALL_DONE, GOAL_ADDED, GOAL_DELETED, IDLE, GREET, LONG_PRESS, DOUBLE_TAP, HOP }

object Phrases {

    val tap = listOf(
        "Hey! That tickles 😆", "Boop!", "Yes? I'm listening 👂", "Hi hi hi!", "You rang? 🔔",
        "*happy noises*", "Hehe, again!", "That's my nose!", "Present! 🙋", "Ouch… just kidding!",
        "High five! ✋", "I felt that in my pixels", "Poke accepted ✅", "Reporting for duty! 🫡",
        "Careful, I'm fragile… like your sleep schedule", "Did you need something?", "Wheee!",
        "Tap tap, who's there?", "I'm awake, I'm awake!", "That was a 10/10 tap", "Hiya, boss!",
        "You found me!", "Beep boop 🤖", "Hehe stop it 🙈", "Ready when you are!"
    )

    val pokeSpam = listOf(
        "OK OK I get it! 😵‍💫", "Is this a goal? Poking me 100 times?", "My head is spinning!",
        "Tap me one more time… I dare you", "Too… many… boops…", "I'm calling my lawyer 🧑‍⚖️",
        "Achievement unlocked: Professional Poker", "Dizzy mode activated 🌀",
        "If only you tapped your goals this fast 👀", "Fine, I'll dance. Happy? 💃", "Error 418: I'm a teapot ☕"
    )

    val scroll = listOf(
        "Wheeee! 🎢", "Scroll scroll scroll…", "Whoa, slow down!", "I'm surfing! 🏄", "Faster! Faster!",
        "Looking for something?", "Weee, motion sickness!", "Zoom zoom 🏎️", "Don't drop me!",
        "Everything's moving!", "Hold on to your goals!", "I love this ride!", "Up and down we go"
    )

    val pageLines = listOf(
        "New page, new me ✨", "Ooh, {page} goals!", "Big picture time 🖼️", "Let's plan the {page}!",
        "Swish! 🌀", "Changing scenery!", "The {page} view looks nice", "Time-travel engaged ⏳",
        "Plan big, start small", "Zooming out… 🔭", "Zooming in… 🔍"
    )

    val goalDone = listOf(
        "YES! \"{goal}\" done! 🎉", "Boom! Another one! 💥", "Look at you go! 🚀", "Check! ✅ So satisfying",
        "You're on fire! 🔥", "That's what I'm talking about!", "Goal crushed 💪", "Victory dance! 💃🕺",
        "Proud of you! 🥹", "Ka-ching! +1 productivity", "Legendary move ⭐", "Done and dusted!",
        "Another brick in the wall of success 🧱", "The checkbox thanks you", "Tick! So clean.",
        "Small win = big deal 🏆", "Nailed it! 🔨", "Mission complete 🎯", "Productivity level: unstoppable",
        "Ooh, that checkmark looks good on you"
    )

    val allDone = listOf(
        "ALL DONE! You're a legend! 👑", "Everything checked! Party time 🥳", "100%! I'm so proud of you!",
        "Clean sweep! 🧹✨", "Nothing left! Go treat yourself 🍦", "Perfect score! 💯",
        "You finished everything?! Teach me your ways", "Hero status achieved 🦸"
    )

    val goalAdded = listOf(
        "New goal? Let's do this! 💪", "Ooh, \"{goal}\" sounds fun!", "Added! I believe in you ✨",
        "Writing it down = half done ✍️", "Challenge accepted! 😤", "Another adventure begins!",
        "I'll keep an eye on that one 👀", "Good one! Future you says thanks", "On the list! 📝",
        "Ambitious! I like it 😎"
    )

    val goalDeleted = listOf(
        "Poof! Gone 💨", "Bye bye, goal 👋", "Decluttering is a skill too!", "Into the void it goes 🕳️",
        "Less is more ✨", "Deleted. We never speak of it again 🤐"
    )

    val longPress = listOf(
        "Wanna change my look? 💅", "Hold me longer and I'll fall asleep 😴", "Ooh, a hug! 🤗",
        "Makeover time?", "I'm a bit shy… 🙈"
    )

    val doubleTap = listOf(
        "Double tap = double fun!", "Spin move! 🌀", "Two taps, one buddy!", "Ta-da! ✨", "Twirl! 💫"
    )

    val hop = listOf(
        "Parkour! 🤸", "I live on the walls now", "Upside-down is my new favorite", "Hop! 🐸",
        "Wall walk activated", "New spot, who dis?", "Exploring! 🧭", "Ceiling view is great"
    )

    val motivation = listOf(
        "Small steps still move you forward 👣", "Done is better than perfect!", "You've got this 💪",
        "One goal at a time 🎯", "Progress, not perfection ✨", "Future you is cheering!",
        "Start where you are 🌱", "Every checkbox counts!", "Consistency beats intensity 🔁",
        "Dream big, plan small, act now", "You're doing better than you think 💜",
        "The best time to start was yesterday. The next best is now!", "Focus on the next tiny step",
        "Believe in your plan 📝", "Momentum is magic 🪄", "Rest is part of the plan too 😌",
        "Your goals called. They miss you ☎️", "Make today count!", "Hard things become easy things",
        "Keep going, you're closer than you think 🏁", "Win the morning, win the day ☀️"
    )

    val goalRemind = listOf(
        "Psst… \"{goal}\" is waiting 👀", "How about \"{goal}\" next?", "{left} goals left. Easy!",
        "\"{goal}\" won't do itself… or will it? 🤔", "I believe \"{goal}\" is your destiny ✨",
        "Only {left} to go! 🏁", "Wanna crush \"{goal}\"?", "{done} done already! Keep the streak 🔥",
        "Tick \"{goal}\" and I'll do a dance", "\"{goal}\" — today's main quest ⚔️",
        "Side quest available: \"{goal}\""
    )

    val funny = listOf(
        "I'm not lazy, I'm in energy-saving mode 🔋", "Do snacks count as a goal? 🍪",
        "My only goal today: look cute 💅", "I tried to exercise once. The couch won.",
        "Why do I live in your phone? Rent is cheap.", "I'm 90% pixels and 10% vibes",
        "Is it nap o'clock yet? 😴", "Brb, contemplating existence", "*hums a little tune* 🎵",
        "I walked on the ceiling today. Personal best.", "If I had legs I'd… oh wait, I do!",
        "Plot twist: I'm the main character", "Loading motivation… 47%… ⏳",
        "Coffee first, goals second ☕", "I'm not short, I'm widget-sized",
        "Ceiling? Walls? It's all floor if you believe", "Fun fact: I can't swim. Or can I? 🏊",
        "I have no idea what I'm doing, but I'm doing it", "My spirit animal is a sleepy cat 🐈",
        "Who keeps scrolling past their goals? 👀", "I'd high five you but… glass screen",
        "I dreamt I was a to-do list. Scary.", "Hydrate or diedrate 💧", "Today I choose chaos… organised chaos",
        "Imagine being a checkbox. Couldn't be me ☑️"
    )

    val facts = listOf(
        "Tip: writing goals down makes you more likely to finish them ✍️",
        "Tip: break big goals into tiny ones!", "Tip: long-press a goal to edit it",
        "Tip: swipe left/right to switch Day, Week, Month & Year",
        "Tip: you can add goals right from the widget ➕", "Tip: tap me twice for a spin!",
        "Tip: mark a goal 'repeat' to get it back every day", "Tip: try different particles in Settings ✨",
        "Tip: the 2-minute rule — if it takes 2 min, do it now!", "Tip: celebrate small wins 🎉",
        "Tip: give important goals a ⭐ priority", "Tip: turn on the lock screen card in Settings 🔒"
    )

    val greetMorning = listOf(
        "Good morning, sunshine! ☀️", "Rise and shine! What's the plan?", "Morning! Coffee or goals first? ☕",
        "A fresh day, a fresh list!", "Morning! I've been waiting for you 😊"
    )
    val greetAfternoon = listOf(
        "Good afternoon! How's it going?", "Afternoon check-in! 🕑", "Hey there! Lunch fuelled? 🥪",
        "Half the day done, let's go!", "Hi again! Ready to tick some boxes?"
    )
    val greetEvening = listOf(
        "Good evening! 🌆", "Evening! Time to wrap things up?", "Hey! How did today go?",
        "Plan tomorrow tonight, thank yourself later", "Evening vibes 🌙"
    )
    val greetNight = listOf(
        "It's late! Don't forget to sleep 😴", "Night owl mode 🦉", "Midnight planner, huh?",
        "Sleep is a goal too 🌙", "Shh… the other apps are sleeping"
    )

    /** Extra lines per personality: Cheerful, Sassy, Chill, Hype, Nerdy, Grumpy-cute, Royal, Pirate */
    val personalityLines = listOf(
        listOf("Today is going to be amazing! 🌈", "I love being your buddy! 💖", "Yay, goals! Yay, you!",
            "Smile! It's free 😁", "Sunshine mode: ON ☀️", "Everything is awesome!"),
        listOf("Oh, NOW you open the app? 💅", "I'm not saying you're slow, but…", "Those goals won't check themselves, hun",
            "Cute list. Now do it.", "I've seen snails with more hustle 🐌", "Bold of you to scroll instead of work"),
        listOf("No rush… just vibes 🌊", "Breathe in… breathe out…", "It's all good, one step at a time",
            "Take it easy, champ 🍃", "Calm mind, clear goals", "Let it flow 🌿"),
        listOf("LET'S GOOOO! 🔥🔥", "BEAST MODE ACTIVATED 💪", "NO DAYS OFF!!", "YOU ARE A MACHINE!",
            "MAXIMUM EFFORT!! 🚀", "TODAY WE CONQUER!!"),
        listOf("Statistically, you're doing great 📊", "Fun fact: honey never spoils 🍯", "E = mc² (Effort = more checks²)",
            "Optimising your productivity algorithm…", "Did you know octopuses have 3 hearts? 🐙", "Hypothesis: you will win today"),
        listOf("Hmph. Fine. I'm proud of you.", "Don't tell anyone I'm cute 😤", "Ugh, mornings…",
            "I'm only helping because I like you. A little.", "Grr. Do your goals. Please. 😾", "Not grumpy, just… concentrated"),
        listOf("Your Majesty's goals await 👑", "The royal court applauds you!", "By royal decree: be awesome",
            "A ruler must rule their to-do list", "Splendid work, my liege!", "Fetch me a goal, good subject!"),
        listOf("Arrr, chart yer course, matey! 🏴‍☠️", "Goals be treasure! 💰", "Hoist the checkboxes!",
            "Ahoy! Land ho! 🏝️", "Yo ho ho and a list of goals", "Swab the deck… I mean, your tasks")
    )

    private val lastUsed = ArrayDeque<String>()

    private fun pickFrom(list: List<String>): String {
        if (list.isEmpty()) return "…"
        val fresh = list.filter { it !in lastUsed }
        val p = (fresh.ifEmpty { list }).random()
        lastUsed.addLast(p)
        if (lastUsed.size > 12) lastUsed.removeFirst()
        return p
    }

    private fun greeting(): List<String> {
        val h = LocalTime.now().hour
        return when (h) {
            in 5..11 -> greetMorning
            in 12..16 -> greetAfternoon
            in 17..21 -> greetEvening
            else -> greetNight
        }
    }

    /**
     * Pick a line for the given moment, respecting the user's phrase settings and the avatar personality.
     */
    fun pick(
        mood: Mood,
        st: AppSettings,
        personality: Int,
        name: String,
        goal: String? = null,
        left: Int = 0,
        done: Int = 0,
        page: String = "day"
    ): String {
        val pool: List<String> = when (mood) {
            Mood.TAP -> if (Random.nextFloat() < 0.25f) personalityLines.getOrElse(personality) { emptyList() } else tap
            Mood.POKE_SPAM -> pokeSpam
            Mood.SCROLL -> scroll
            Mood.PAGE -> pageLines
            Mood.GOAL_DONE -> goalDone
            Mood.ALL_DONE -> allDone
            Mood.GOAL_ADDED -> goalAdded
            Mood.GOAL_DELETED -> goalDeleted
            Mood.LONG_PRESS -> longPress
            Mood.DOUBLE_TAP -> doubleTap
            Mood.HOP -> hop
            Mood.GREET -> greeting()
            Mood.IDLE -> {
                val buckets = mutableListOf<List<String>>()
                buckets += personalityLines.getOrElse(personality) { emptyList() }
                if (st.phraseFunny) { buckets += funny; buckets += funny }
                if (st.phraseMotivation) { buckets += motivation; buckets += motivation }
                if (st.phraseGoals && goal != null) { buckets += goalRemind; buckets += goalRemind; buckets += goalRemind }
                if (st.phraseFacts) buckets += facts
                buckets += greeting()
                buckets.filter { it.isNotEmpty() }.random()
            }
        }
        var line = pickFrom(pool)
        if (line.contains("{goal}") && goal == null) line = pickFrom(motivation)
        return line
            .replace("{name}", name)
            .replace("{goal}", goal?.let { if (it.length > 28) it.take(26) + "…" else it } ?: "")
            .replace("{left}", left.toString())
            .replace("{done}", done.toString())
            .replace("{page}", page)
    }
}
