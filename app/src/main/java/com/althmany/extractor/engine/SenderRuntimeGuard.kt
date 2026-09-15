package com.althmany.extractor.engine

import android.content.Context
import com.althmany.groupmanager.GroupManagerApp

/**
 * Cross-feature persisted guard for the legacy Sender runtime.
 *
 * RuntimeOperationCoordinator protects the current process, while this guard also checks the
 * persisted Sender batch flag so controller entry points cannot race a Sender batch restored after
 * Activity/service recreation.
 */
object SenderRuntimeGuard {
    fun isSenderRunning(context: Context): Boolean {
        val app = context.applicationContext as? GroupManagerApp ?: return false
        return app.preferences.accessibilityBatchRunning
    }
}
