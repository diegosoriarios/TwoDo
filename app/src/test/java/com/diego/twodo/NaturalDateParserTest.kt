package com.diego.twodo

import com.diego.twodo.domain.parser.NaturalDateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class NaturalDateParserTest {

    private val friday: LocalDate = LocalDate.of(2026, 11, 13)
    private val saturday: LocalDate = LocalDate.of(2026, 11, 14)

    private fun parse(text: String, today: LocalDate = friday) =
        NaturalDateParser.parse(text, today)?.let {
            LocalDate.ofEpochDay(it.epochDay) to it.range
        }

    @Test
    fun `today expressions resolve to the reference date`() {
        assertEquals(friday, parse("buy milk today")?.first)
        assertEquals(friday, parse("comprar leite hoje")?.first)
    }

    @Test
    fun `tomorrow expressions resolve to the next day`() {
        val expected = friday.plusDays(1)
        assertEquals(expected, parse("buy milk tomorrow")?.first)
        assertEquals(expected, parse("buy milk tmr")?.first)
        assertEquals(expected, parse("comprar leite amanhã")?.first)
        assertEquals(expected, parse("comprar leite amanha")?.first)
    }

    @Test
    fun `next week expressions resolve to seven days ahead`() {
        val expected = friday.plusDays(7)
        assertEquals(expected, parse("plan trip next week")?.first)
        assertEquals(expected, parse("plan trip next  week")?.first)
        assertEquals(expected, parse("viajar próxima semana")?.first)
        assertEquals(expected, parse("viajar proxima semana")?.first)
        assertEquals(expected, parse("viajar semana que vem")?.first)
    }

    @Test
    fun `weekend expressions resolve to next saturday`() {
        val expected = saturday
        assertEquals(expected, parse("clean garage weekend")?.first)
        assertEquals(expected, parse("clean garage this weekend")?.first)
        assertEquals(expected, parse("limpar garagem fim de semana")?.first)
        assertEquals(expected, parse("limpar garagem final de semana")?.first)
    }

    @Test
    fun `weekend on a saturday resolves to the same day`() {
        assertEquals(saturday, parse("clean garage weekend", saturday)?.first)
    }

    @Test
    fun `weekday names resolve to the next occurrence`() {
        assertEquals(LocalDate.of(2026, 11, 16), parse("meeting monday")?.first)
        assertEquals(LocalDate.of(2026, 11, 16), parse("reunião segunda")?.first)
        assertEquals(LocalDate.of(2026, 11, 16), parse("reunião segunda-feira")?.first)
        assertEquals(LocalDate.of(2026, 11, 17), parse("reunião terça")?.first)
        assertEquals(LocalDate.of(2026, 11, 15), parse("almoço domingo")?.first)
        assertEquals(LocalDate.of(2026, 11, 14), parse("show sábado")?.first)
        assertEquals(LocalDate.of(2026, 11, 14), parse("show sabado")?.first)
    }

    @Test
    fun `weekday on the reference day rolls to next week`() {
        assertEquals(friday.plusDays(7), parse("report friday", friday)?.first)
    }

    @Test
    fun `relative day expressions resolve by count`() {
        assertEquals(friday.plusDays(3), parse("call back in 3 days")?.first)
        assertEquals(friday.plusDays(3), parse("ligar de volta em 3 dias")?.first)
        assertEquals(friday.plusDays(3), parse("ligar de volta daqui a 3 dias")?.first)
        assertEquals(friday.plusDays(1), parse("call back in 1 day")?.first)
    }

    @Test
    fun `relative week expressions resolve by count`() {
        assertEquals(friday.plusDays(14), parse("review notes in 2 weeks")?.first)
        assertEquals(friday.plusDays(14), parse("revisar notas em 2 semanas")?.first)
        assertEquals(friday.plusDays(14), parse("revisar notas daqui a 2 semanas")?.first)
    }

    @Test
    fun `match range covers exactly the expression`() {
        val (_, range) = parse("buy milk tomorrow")!!
        assertEquals("tomorrow", "buy milk tomorrow".substring(range))
    }

    @Test
    fun `stripping the range leaves the title without the date word`() {
        val text = "buy milk tomorrow"
        val (_, range) = parse(text)!!
        assertEquals("buy milk ", text.removeRange(range))
    }

    @Test
    fun `matching is case insensitive`() {
        assertEquals(friday.plusDays(1), parse("BUY MILK TOMORROW")?.first)
        assertEquals(friday.plusDays(7), parse("Viajar Próxima Semana")?.first)
    }

    @Test
    fun `partial words do not match`() {
        assertNull(parse("buy milk tod"))
        assertNull(parse("comprar leite ama"))
    }

    @Test
    fun `text without a date expression returns null`() {
        assertNull(parse("buy milk"))
        assertNull(parse(""))
    }

    @Test
    fun `expression embedded in a word does not match`() {
        assertNull(parse("todays homework"))
        assertNull(parse("os sábados são bons"))
    }

    @Test
    fun `earliest expression wins`() {
        val (date, range) = parse("today or tomorrow")!!
        assertEquals(friday, date)
        assertEquals(0, range.first)
    }
}
