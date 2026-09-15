package com.althmany.extractor

import android.content.Context
import com.althmany.extractor.data.ExtractorDatabase
import com.althmany.extractor.data.ExtractorRepository
import com.althmany.extractor.engine.ExtractionController
import com.althmany.extractor.engine.PublishController
import com.althmany.extractor.engine.ScanController

/**
 * Feature runtime embedded inside AL-thmany Sender.
 * Sender remains the only Application class; this object owns the Extractor database/controllers.
 */
object ExtractorFeatureRuntime {
    @Volatile private var initialized = false

    lateinit var repository: ExtractorRepository
        private set

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        val database = ExtractorDatabase(appContext)
        repository = ExtractorRepository(database)
        ExtractionController.initialize(appContext, repository)
        ScanController.initialize(appContext, repository)
        PublishController.initialize(appContext, repository)
        initialized = true
    }
}
