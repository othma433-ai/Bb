package com.althmany.extractor.profile

/**
 * Database isolation policy for AL-thmany runtime environments.
 *
 * Local execution keeps the historic database name so existing user data is preserved.
 * A Shizuku target in another Android user gets its own database file in the host app sandbox.
 * WhatsApp Personal/Business/Dual inside the same Android user remain partitioned by the existing
 * whatsapp_package column.
 */
object EnvironmentDatabasePolicy {
    const val LOCAL_DATABASE = "althmany_extractor.db"

    fun databaseName(remoteTarget: Boolean, androidUserId: Int): String {
        require(androidUserId >= 0) { "Android user id must be non-negative" }
        return if (remoteTarget) {
            "althmany_extractor_remote_u$androidUserId.db"
        } else {
            LOCAL_DATABASE
        }
    }

    fun scopeKey(remoteTarget: Boolean, androidUserId: Int): String =
        if (remoteTarget) "REMOTE:u$androidUserId" else "LOCAL"
}
