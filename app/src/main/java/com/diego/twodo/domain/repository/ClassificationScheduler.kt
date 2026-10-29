package com.diego.twodo.domain.repository

interface ClassificationScheduler {
    /**
     * Schedules best-effort LLM classification for any queued unclassified tasks.
     */
    fun schedule()
}
