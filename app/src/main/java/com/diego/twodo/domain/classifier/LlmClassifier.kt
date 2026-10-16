package com.diego.twodo.domain.classifier

import com.diego.twodo.domain.model.ClassificationResult

interface LlmClassifier {
    /**
     * Classifies a task title using a local LLM model.
     * Enforces strict JSON output: {"category": "2m" | "30m" | "long", "reason": "string"}
     * Conservative rule: "If unsure between 2m and 30m, select 30m".
     */
    suspend fun classifyTask(taskTitle: String): ClassificationResult?

    /**
     * Checks if local LLM model files are available and initialized.
     */
    fun isModelLoaded(): Boolean
}
