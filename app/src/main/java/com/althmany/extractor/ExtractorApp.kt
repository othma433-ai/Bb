package com.althmany.extractor

import android.content.Context
import com.althmany.extractor.data.ExtractorDatabase
import com.althmany.extractor.data.ExtractorRepository
import com.althmany.extractor.engine.ExtractionController
import com.althmany.extractor.engine.PublishController
import com.althmany.extractor.engine.ScanController
import com.althmany.extractor.profile.EnvironmentDatabasePolicy
import com.althmany.extractor.profile.UnifiedRuntimeTargetStore

/**
 * Feature runtime embedded inside AL-thmany Sender.
 *
 * Data isolation rule:
 * - Local Android profile: keep the historic althmany_extractor.db. Android already isolates the
 *   application sandbox per user/profile, so a copy installed in Work/Secure has its own local DB.
 * - Shizuku target in another Android user: use a dedicated database file per Android user.
 *
 * Switching is allowed only while extractor/scan/publish are idle. The legacy Join engine has its
 * own repository/session model and is guarded by AppViewModel before target changes.
 */
object ExtractorFeatureRuntime {
    @Volatile private var initialized = false
    @Volatile private var activeScopeKey: String = ""

    private val databases = linkedMapOf<String, ExtractorDatabase>()
    private val repositories = linkedMapOf<String, ExtractorRepository>()

    lateinit var repository: ExtractorRepository
        private set

    @Synchronized
    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val target = UnifiedRuntimeTargetStore.resolve(appContext)
        val scope = EnvironmentDatabasePolicy.scopeKey(
            target.remoteTarget,
            target.targetAndroidUserId
        )

        if (initialized && scope == activeScopeKey) return
        if (initialized) {
            switchRuntimeEnvironment(appContext)
            return
        }

        openScope(
            appContext = appContext,
            remoteTarget = target.remoteTarget,
            androidUserId = target.targetAndroidUserId
        )
        initialized = true
    }

    /**
     * Rebinds all extractor-side engines to the database that belongs to the currently selected
     * unified runtime environment. Returns false instead of switching under an active operation.
     */
    @Synchronized
    fun switchRuntimeEnvironment(context: Context): Boolean {
        val appContext = context.applicationContext
        if (
            ExtractionController.isBusy() ||
            ScanController.isRunning() ||
            PublishController.isRunning()
        ) return false

        val target = UnifiedRuntimeTargetStore.resolve(appContext)
        val nextScope = EnvironmentDatabasePolicy.scopeKey(
            target.remoteTarget,
            target.targetAndroidUserId
        )
        if (initialized && nextScope == activeScopeKey) return true

        openScope(
            appContext = appContext,
            remoteTarget = target.remoteTarget,
            androidUserId = target.targetAndroidUserId
        )
        initialized = true
        return true
    }

    fun currentScopeKey(): String = activeScopeKey

    private fun openScope(
        appContext: Context,
        remoteTarget: Boolean,
        androidUserId: Int
    ) {
        val scopeKey = EnvironmentDatabasePolicy.scopeKey(remoteTarget, androidUserId)
        val dbName = EnvironmentDatabasePolicy.databaseName(remoteTarget, androidUserId)
        val scopedDatabase = databases.getOrPut(scopeKey) {
            ExtractorDatabase(appContext, dbName)
        }
        val scopedRepository = repositories.getOrPut(scopeKey) {
            ExtractorRepository(scopedDatabase)
        }

        repository = scopedRepository
        activeScopeKey = scopeKey

        ExtractionController.initialize(appContext, scopedRepository)
        ScanController.initialize(appContext, scopedRepository)
        PublishController.initialize(appContext, scopedRepository)
    }
}
