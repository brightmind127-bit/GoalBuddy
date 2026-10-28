package com.goalbuddy.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object Periods {

    fun dateFor(period: Period, offset: Int, today: LocalDate = LocalDate.now()): LocalDate =
        when (period) {
            Period.DAY -> today.plusDays(offset.toLong())
            Period.WEEK -> today.plusWeeks(offset.toLong())
            Period.MONTH -> today.plusMonths(offset.toLong())
            Period.YEAR -> today.plusYears(offset.toLong())
        }

    fun key(period: Period, date: LocalDate): String = when (period) {
        Period.DAY -> date.toString()
        Period.WEEK -> "%d-W%02d".format(
            date.get(IsoFields.WEEK_BASED_YEAR),
            date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        )
        Period.MONTH -> "%d-%02d".format(date.year, date.monthValue)
        Period.YEAR -> date.year.toString()
    }

    fun keyFor(period: Period, offset: Int): String = key(period, dateFor(period, offset))

    fun currentKey(period: Period): String = key(period, LocalDate.now())

    /** Big title for a page, e.g. "Today", "This week", "October 2026" */
    fun title(period: Period, offset: Int): String {
        val d = dateFor(period, offset)
        return when (period) {
            Period.DAY -> when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                -1 -> "Yesterday"
                else -> d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
            Period.WEEK -> when (offset) {
                0 -> "This week"
                1 -> "Next week"
                -1 -> "Last week"
                else -> "Week ${d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
            }
            Period.MONTH -> when (offset) {
                0 -> "This month"
                1 -> "Next month"
                -1 -> "Last month"
                else -> d.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
            Period.YEAR -> when (offset) {
                0 -> "This year"
                1 -> "Next year"
                -1 -> "Last year"
                else -> d.year.toString()
            }
        }
    }

    /** Small line under the title, e.g. "Tue, Oct 27" or "Oct 26 – Nov 1" */
    fun subtitle(period: Period, offset: Int): String {
        val d = dateFor(period, offset)
        return when (period) {
            Period.DAY -> d.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
            Period.WEEK -> {
                val start = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = start.plusDays(6)
                val f = DateTimeFormatter.ofPattern("MMM d")
                "Week ${d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} · ${start.format(f)} – ${end.format(f)}"
            }
            Period.MONTH -> d.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            Period.YEAR -> {
                val dayOfYear = LocalDate.now().dayOfYear
                if (offset == 0) "${d.year} · day $dayOfYear of ${d.lengthOfYear()}" else d.year.toString()
            }
        }
    }

    /** How far through the current period we are (0..1), used for the time bar. */
    fun elapsedFraction(period: Period): Float {
        val now = java.time.LocalDateTime.now()
        val d = now.toLocalDate()
        val dayFrac = (now.hour * 60 + now.minute) / 1440f
        return when (period) {
            Period.DAY -> dayFrac
            Period.WEEK -> ((d.dayOfWeek.value - 1) + dayFrac) / 7f
            Period.MONTH -> ((d.dayOfMonth - 1) + dayFrac) / d.lengthOfMonth()
            Period.YEAR -> ((d.dayOfYear - 1) + dayFrac) / d.lengthOfYear()
        }.coerceIn(0f, 1f)
    }
}
