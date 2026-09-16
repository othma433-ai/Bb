package com.althmany.extractor.engine

import com.althmany.extractor.data.ScanStatus

object ScanRetryPolicy {
    /**
     * One explicit Scan run classifies each queued URL once.
     * Network loss is handled by the runtime pause/wait path before final classification,
     * so it must not reopen the same URL as a second scan attempt.
     */
    @Suppress("UNUSED_PARAMETER")
    fun shouldRetry(status: ScanStatus, attempt: Int, maxAttempts: Int): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun backoffMs(status: ScanStatus, attempt: Int, speed: ScanSpeedProfile): Long = 0L
}
