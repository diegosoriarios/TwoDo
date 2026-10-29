package com.diego.twodo.domain.classifier

interface LlmInferenceEngine {
    /**
     * Loads the model if needed and returns whether the engine is ready for inference.
     */
    suspend fun warmUp(modelPath: String): Boolean

    /**
     * Runs a one-shot prompt and returns the raw model response, or null on failure.
     */
    suspend fun generate(
        modelPath: String,
        systemInstruction: String,
        userPrompt: String,
    ): String?

    /**
     * Releases native resources. Safe to call multiple times.
     */
    fun close()
}
