package com.diego.twodo.data.classifier

import com.diego.twodo.domain.classifier.LlmClassifier
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.ClassificationResult
import com.diego.twodo.domain.model.TaskBucket
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalLlmClassifier @Inject constructor(
    private val heuristicClassifier: HeuristicClassifier
) : LlmClassifier {

    private var isModelInitialized: Boolean = false

    fun setModelInitialized(initialized: Boolean) {
        isModelInitialized = initialized
    }

    override fun isModelLoaded(): Boolean = isModelInitialized

    override suspend fun classifyTask(taskTitle: String): ClassificationResult? {
        if (!isModelInitialized) {
            return null
        }

        val rawLlmResponse = executeInference(buildPrompt(taskTitle)) ?: return null
        return parseResponse(rawLlmResponse)
    }

    private fun buildPrompt(taskTitle: String): String {
        return """
            System: Categorize the task into one of these time buckets:
            - "2m": Quick task <= 2 minutes.
            - "30m": Focused session ~15-30 minutes.
            - "long": Complex project > 30 minutes.
            Rule: If unsure between 2m and 30m, choose "30m".
            Output JSON: {"category": "2m"|"30m"|"long", "reason": "explanation"}
            Task: "$taskTitle"
        """.trimIndent()
    }

    private suspend fun executeInference(prompt: String): String? {
        // Internal LLM engine runner placeholder (connected to MediaPipe / Native local GGUF)
        return try {
            // Simulated structured response or actual native output
            null
        } catch (e: Exception) {
            null
        }
    }

    fun parseResponse(rawResponse: String): ClassificationResult {
        return try {
            val cleanJson = rawResponse.trim()
                .substringAfter("{")
                .substringBeforeLast("}")
            val fullJson = "{$cleanJson}"
            val jsonObject = JSONObject(fullJson)
            val categoryStr = jsonObject.optString("category", "30m").lowercase()
            val reason = jsonObject.optString("reason", "Classified by local LLM")

            val bucket = when (categoryStr) {
                "2m", "2_min", "two_min" -> TaskBucket.TWO_MIN
                "30m", "thirty_min" -> TaskBucket.THIRTY_MIN
                "long" -> TaskBucket.LONG
                else -> TaskBucket.THIRTY_MIN // Conservative default rule
            }

            ClassificationResult(
                category = bucket,
                reason = reason,
                source = BucketSource.LLM
            )
        } catch (e: Exception) {
            ClassificationResult(
                category = TaskBucket.THIRTY_MIN, // Conservative fallback
                reason = "LLM response parsing error; default conservative 30m applied",
                source = BucketSource.LLM
            )
        }
    }
}
