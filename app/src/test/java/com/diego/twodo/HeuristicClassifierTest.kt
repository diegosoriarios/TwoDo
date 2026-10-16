package com.diego.twodo

import com.diego.twodo.data.classifier.HeuristicClassifier
import com.diego.twodo.domain.model.TaskBucket
import org.junit.Assert.assertEquals
import org.junit.Test

class HeuristicClassifierTest {

    private val classifier = HeuristicClassifier()

    @Test
    fun `classify 2-minute task keywords`() {
        val result1 = classifier.classify("call mom")
        assertEquals(TaskBucket.TWO_MIN, result1.category)

        val result2 = classifier.classify("pay parking ticket")
        assertEquals(TaskBucket.TWO_MIN, result2.category)

        val result3 = classifier.classify("reply to email from john")
        assertEquals(TaskBucket.TWO_MIN, result3.category)
    }

    @Test
    fun `classify 30-minute task keywords`() {
        val result1 = classifier.classify("draft weekly project status report")
        assertEquals(TaskBucket.THIRTY_MIN, result1.category)

        val result2 = classifier.classify("review pull request")
        assertEquals(TaskBucket.THIRTY_MIN, result2.category)
    }

    @Test
    fun `classify long project keywords`() {
        val result1 = classifier.classify("organize garage and sell old furniture")
        assertEquals(TaskBucket.LONG, result1.category)

        val result2 = classifier.classify("plan summer vacation itinerary with family")
        assertEquals(TaskBucket.LONG, result2.category)
    }

    @Test
    fun `ambiguous task returns UNCLASSIFIED`() {
        val result = classifier.classify("buy groceries and items")
        assertEquals(TaskBucket.UNCLASSIFIED, result.category)
    }
}
