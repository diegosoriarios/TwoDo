package com.diego.twodo.domain.parser

import java.time.DayOfWeek
import java.time.LocalDate

/** A date expression found in free-form text, e.g. "tomorrow" inside "buy milk tomorrow". */
data class NaturalDateMatch(
    val epochDay: Long,
    val range: IntRange,
)

/**
 * Recognizes Todoist-style date expressions in task titles (English and Brazilian
 * Portuguese, regardless of device locale) and resolves them against a reference date.
 * Callers are expected to strip [NaturalDateMatch.range] from the original text.
 */
object NaturalDateParser {

    private const val START = "(?<![\\p{L}\\p{N}])"
    private const val END = "(?![\\p{L}\\p{N}])"

    private val fixedRegex = Regex(
        START + "(today|hoje" +
            "|tomorrow|tmr|amanhã|amanha" +
            "|next\\s+week|próxima\\s+semana|proxima\\s+semana|semana\\s+que\\s+vem" +
            "|(?:this\\s+)?weekend|fim\\s+de\\s+semana|final\\s+de\\s+semana)" + END,
        RegexOption.IGNORE_CASE,
    )

    private val weekdayRegex = Regex(
        START + "(monday|tuesday|wednesday|thursday|friday|saturday|sunday" +
            "|segunda(?:-feira)?|terça(?:-feira)?|quarta(?:-feira)?|quinta(?:-feira)?|sexta(?:-feira)?" +
            "|sábado|sabado|domingo)" + END,
        RegexOption.IGNORE_CASE,
    )

    private val nUnitRegex = Regex(
        START + "(?:in|em|daqui\\s+a)\\s+(\\d{1,2})\\s+(days?|dias?|weeks?|semanas?)" + END,
        RegexOption.IGNORE_CASE,
    )

    private val patterns = listOf(
        fixedRegex to ::resolveFixed,
        weekdayRegex to ::resolveWeekday,
        nUnitRegex to ::resolveNUnit,
    )

    fun parse(text: String, today: LocalDate = LocalDate.now()): NaturalDateMatch? =
        patterns.asSequence()
            .mapNotNull { (regex, resolve) -> regex.find(text)?.let { m -> m.range to resolve(m, today) } }
            .filter { it.second != null }
            .minWithOrNull(compareBy({ it.first.first }, { -it.first.last }))
            ?.let { (range, date) -> NaturalDateMatch(requireNotNull(date).toEpochDay(), range) }

    private fun resolveFixed(match: MatchResult, today: LocalDate): LocalDate? =
        when (match.value.lowercase().replace(Regex("\\s+"), " ")) {
            "today", "hoje" -> today
            "tomorrow", "tmr", "amanhã", "amanha" -> today.plusDays(1)
            "next week", "próxima semana", "proxima semana", "semana que vem" -> today.plusDays(7)
            "weekend", "this weekend", "fim de semana", "final de semana" -> nextSaturday(today)
            else -> null
        }

    private fun resolveWeekday(match: MatchResult, today: LocalDate): LocalDate? {
        val day = weekdayOf(match.value) ?: return null
        val offset = ((day.value - today.dayOfWeek.value + 7) % 7).toLong()
        return today.plusDays(if (offset == 0L) 7L else offset)
    }

    private fun weekdayOf(value: String): DayOfWeek? =
        when (value.lowercase().removeSuffix("-feira")) {
            "monday", "segunda" -> DayOfWeek.MONDAY
            "tuesday", "terça" -> DayOfWeek.TUESDAY
            "wednesday", "quarta" -> DayOfWeek.WEDNESDAY
            "thursday", "quinta" -> DayOfWeek.THURSDAY
            "friday", "sexta" -> DayOfWeek.FRIDAY
            "saturday", "sábado", "sabado" -> DayOfWeek.SATURDAY
            "sunday", "domingo" -> DayOfWeek.SUNDAY
            else -> null
        }

    private fun resolveNUnit(match: MatchResult, today: LocalDate): LocalDate? {
        val count = match.groupValues[1].toIntOrNull() ?: return null
        val days = when (match.groupValues[2].lowercase()) {
            "day", "days", "dia", "dias" -> count.toLong()
            "week", "weeks", "semana", "semanas" -> count * 7L
            else -> return null
        }
        return today.plusDays(days)
    }

    private fun nextSaturday(today: LocalDate): LocalDate = when (today.dayOfWeek) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> today
        else -> today.plusDays(((DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7).toLong())
    }
}
