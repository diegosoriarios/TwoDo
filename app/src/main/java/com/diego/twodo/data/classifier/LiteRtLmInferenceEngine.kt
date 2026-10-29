package com.diego.twodo.data.classifier

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.diego.twodo.domain.classifier.LlmInferenceEngine
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ResponseFormat
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LiteRtLmInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : LlmInferenceEngine {

    private val lock = Any()

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var loadedModelPath: String? = null

    @Volatile
    private var failedModelPath: String? = null

    override suspend fun warmUp(modelPath: String): Boolean = withContext(Dispatchers.Default) {
        synchronized(lock) {
            if (failedModelPath == modelPath) {
                failedModelPath = null
            }
            ensureEngine(modelPath) != null
        }
    }

    override suspend fun generate(
        modelPath: String,
        systemInstruction: String,
        userPrompt: String,
    ): String? = withContext(Dispatchers.Default) {
        synchronized(lock) {
            val activeEngine = ensureEngine(modelPath) ?: return@withContext null

            try {
                createConversation(
                    engine = activeEngine,
                    systemInstruction = systemInstruction,
                    constrained = true,
                ).use { conversation ->
                    conversation.sendMessage(
                        userPrompt,
                        responseFormat = RESPONSE_FORMAT,
                    ).toString()
                }
            } catch (throwable: Throwable) {
                Log.w(TAG, "Constrained generation failed, retrying without JSON constraints", throwable)
                try {
                    createConversation(
                        engine = activeEngine,
                        systemInstruction = systemInstruction,
                        constrained = false,
                    ).use { conversation ->
                        conversation.sendMessage(userPrompt).toString()
                    }
                } catch (fallbackFailure: Throwable) {
                    Log.e(TAG, "On-device model inference failed", fallbackFailure)
                    null
                }
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            engine?.let { activeEngine ->
                try {
                    if (activeEngine.isInitialized()) activeEngine.close()
                } catch (throwable: Throwable) {
                    Log.w(TAG, "Failed to close LiteRT-LM engine", throwable)
                }
            }
            engine = null
            loadedModelPath = null
            failedModelPath = null
        }
    }

    private fun ensureEngine(modelPath: String): Engine? {
        val current = engine
        if (current != null && loadedModelPath == modelPath && current.isInitialized()) {
            return current
        }
        if (failedModelPath == modelPath) {
            return null
        }

        if (current != null) {
            try {
                if (current.isInitialized()) current.close()
            } catch (throwable: Throwable) {
                Log.w(TAG, "Failed to close previous LiteRT-LM engine", throwable)
            }
        }
        engine = null
        loadedModelPath = null

        var created: Engine? = null
        return try {
            if (!File(modelPath).isFile) {
                failedModelPath = modelPath
                return null
            }

            val cacheDirectory = File(context.cacheDir, CACHE_DIRECTORY).apply { mkdirs() }
            created = Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = cacheDirectory.absolutePath,
                    maxNumTokens = MAX_CONTEXT_TOKENS,
                ),
            )
            created.initialize()
            engine = created
            loadedModelPath = modelPath
            failedModelPath = null
            created
        } catch (throwable: Throwable) {
            Log.e(TAG, "Failed to initialize LiteRT-LM engine", throwable)
            try {
                val engineToClose = created
                if (engineToClose != null && engineToClose.isInitialized()) {
                    engineToClose.close()
                }
            } catch (closeFailure: Throwable) {
                Log.w(TAG, "Failed to clean up LiteRT-LM engine", closeFailure)
            }
            failedModelPath = modelPath
            null
        }
    }

    private fun createConversation(
        engine: Engine,
        systemInstruction: String,
        constrained: Boolean,
    ) = engine.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            samplerConfig = SamplerConfig(
                topK = 1,
                topP = 1.0,
                temperature = 0.0,
            ),
            maxOutputToken = MAX_OUTPUT_TOKENS,
            thinkingConfig = ThinkingConfig(
                enableThinking = false,
                thinkingTokenBudget = 0,
            ),
            enableResponseFormat = constrained,
        ),
    )

    companion object {
        private const val TAG = "LiteRtLmEngine"
        private const val CACHE_DIRECTORY = "litertlm"
        private const val MAX_CONTEXT_TOKENS = 1024
        private const val MAX_OUTPUT_TOKENS = 96

        private const val JSON_SCHEMA =
            """{"type":"object","properties":{"category":{"type":"string","enum":["2m","30m","long"]},"reason":{"type":"string"}},"required":["category","reason"]}"""

        @VisibleForTesting
        val RESPONSE_FORMAT: ResponseFormat = ResponseFormat.json(JSON_SCHEMA)
    }
}
