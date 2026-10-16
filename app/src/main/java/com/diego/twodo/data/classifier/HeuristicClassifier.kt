package com.diego.twodo.data.classifier

import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.ClassificationResult
import com.diego.twodo.domain.model.TaskBucket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicClassifier @Inject constructor() {

    private val twoMinRegex = Regex(
        "(?i)^\\b(call|text|reply|email|mail|pay|unsub|unsubscribe|send|ping|message|sign|forward|confirm|venmo|zelle|tweet|check|verify|ask)\\b"
    )

    private val thirtyMinRegex = Regex(
        "(?i)^\\b(draft|review|read|summarize|outline|skim|order|water|walk|workout|exercise|stretch|file|backup|clean)\\b"
    )

    private val longRegex = Regex(
        "(?i)^\\b(organize|plan|build|design|refactor|migrate|remodel|renovate|prepare|research|learn|develop|setup|install)\\b"
    )

    fun classify(title: String): ClassificationResult {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return ClassificationResult(
                category = TaskBucket.UNCLASSIFIED,
                reason = "Empty task title",
                source = BucketSource.HEURISTIC
            )
        }

        val wordCount = trimmed.split("\\s+".toRegex()).size

        // Direct regex matches
        when {
            twoMinRegex.containsMatchIn(trimmed) && wordCount <= 6 -> {
                return ClassificationResult(
                    category = TaskBucket.TWO_MIN,
                    reason = "Matched quick action keyword (<=2 min)",
                    source = BucketSource.HEURISTIC
                )
            }
            longRegex.containsMatchIn(trimmed) || wordCount > 12 -> {
                return ClassificationResult(
                    category = TaskBucket.LONG,
                    reason = "Matched complex project keyword or long task (>30 min)",
                    source = BucketSource.HEURISTIC
                )
            }
            thirtyMinRegex.containsMatchIn(trimmed) -> {
                return ClassificationResult(
                    category = TaskBucket.THIRTY_MIN,
                    reason = "Matched single-session focused keyword (~15-30 min)",
                    source = BucketSource.HEURISTIC
                )
            }
            else -> {
                return ClassificationResult(
                    category = TaskBucket.UNCLASSIFIED,
                    reason = "Ambiguous task - scheduled for local LLM classification",
                    source = BucketSource.HEURISTIC
                )
            }
        }
    }
}
