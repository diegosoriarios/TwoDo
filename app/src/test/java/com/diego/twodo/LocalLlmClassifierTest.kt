package com.diego.twodo

import com.diego.twodo.data.classifier.LocalLlmClassifier
import com.diego.twodo.domain.classifier.LlmInferenceEngine
import com.diego.twodo.domain.model.BucketSource
import com.diego.twodo.domain.model.TaskBucket
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLlmClassifierTest {

    @Test
    fun `returns null when no model path is configured`() = runBlocking {
        val engine = FakeLlmInferenceEngine()
        val classifier = LocalLlmClassifier(engine)

        val result = classifier.classifyTask("call mom")

        assertNull(result)
        assertFalse(engine.generateCalled)
    }

    @Test
    fun `parses a valid response`() = runBlocking {
        val engine = FakeLlmInferenceEngine(
            response = """{"category":"2m","reason":"Quick call"}""",
        )
        val classifier = LocalLlmClassifier(engine)
        classifier.setModelPath("/model.litertlm")

        val result = classifier.classifyTask("call mom")

        assertEquals(TaskBucket.TWO_MIN, result?.category)
        assertEquals("Quick call", result?.reason)
        assertEquals(BucketSource.LLM, result?.source)
    }

    @Test
    fun `parses json wrapped in markdown fences`() {
        val classifier = LocalLlmClassifier(FakeLlmInferenceEngine())

        val result = classifier.parseResponse(
            "```json\n{\"category\":\"long\",\"reason\":\"Project\"}\n```",
        )

        assertEquals(TaskBucket.LONG, result.category)
    }

    @Test
    fun `malformed responses fall back conservatively to 30m`() {
        val classifier = LocalLlmClassifier(FakeLlmInferenceEngine())

        val result = classifier.parseResponse("not json")

        assertEquals(TaskBucket.THIRTY_MIN, result.category)
    }

    @Test
    fun `sanitizes the task title before inference`() = runBlocking {
        val engine = FakeLlmInferenceEngine(
            response = """{"category":"30m","reason":"Focused"}""",
        )
        val classifier = LocalLlmClassifier(engine)
        classifier.setModelPath("/model.litertlm")

        classifier.classifyTask("review\n\npull\trequest")

        assertEquals("Task title: \"review pull request\"", engine.lastUserPrompt)
        assertTrue(engine.lastSystemInstruction?.contains("2m") == true)
    }

    @Test
    fun `warm up delegates to the engine when the model path is set`() = runBlocking {
        val engine = FakeLlmInferenceEngine(warmUpResult = true)
        val classifier = LocalLlmClassifier(engine)

        assertFalse(classifier.warmUp())

        classifier.setModelPath("/model.litertlm")

        assertTrue(classifier.warmUp())
        assertEquals("/model.litertlm", engine.lastWarmUpPath)
    }

    private class FakeLlmInferenceEngine(
        private val response: String? = null,
        private val warmUpResult: Boolean = false,
    ) : LlmInferenceEngine {
        var generateCalled = false
        var lastUserPrompt: String? = null
        var lastSystemInstruction: String? = null
        var lastWarmUpPath: String? = null

        override suspend fun warmUp(modelPath: String): Boolean {
            lastWarmUpPath = modelPath
            return warmUpResult
        }

        override suspend fun generate(
            modelPath: String,
            systemInstruction: String,
            userPrompt: String,
        ): String? {
            generateCalled = true
            lastSystemInstruction = systemInstruction
            lastUserPrompt = userPrompt
            return response
        }

        override fun close() = Unit
    }
}
