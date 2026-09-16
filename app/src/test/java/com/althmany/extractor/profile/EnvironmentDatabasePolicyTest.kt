package com.althmany.extractor.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EnvironmentDatabasePolicyTest {
    @Test
    fun localKeepsHistoricDatabaseName() {
        assertEquals(
            "althmany_extractor.db",
            EnvironmentDatabasePolicy.databaseName(remoteTarget = false, androidUserId = 0)
        )
        assertEquals(
            "althmany_extractor.db",
            EnvironmentDatabasePolicy.databaseName(remoteTarget = false, androidUserId = 10)
        )
    }

    @Test
    fun remoteAndroidUsersArePhysicallyIsolated() {
        val work = EnvironmentDatabasePolicy.databaseName(remoteTarget = true, androidUserId = 10)
        val secure = EnvironmentDatabasePolicy.databaseName(remoteTarget = true, androidUserId = 150)
        assertEquals("althmany_extractor_remote_u10.db", work)
        assertEquals("althmany_extractor_remote_u150.db", secure)
        assertNotEquals(work, secure)
    }
}
