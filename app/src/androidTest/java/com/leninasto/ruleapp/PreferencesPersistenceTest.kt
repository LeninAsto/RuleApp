package com.leninasto.ruleapp

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.leninasto.ruleapp.data.local.PreferencesRepository
import com.leninasto.ruleapp.domain.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PreferencesPersistenceTest {
    @Test fun versionAcknowledgementAndLastVisitSurviveStoreReopening() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "preferences-qa-${System.nanoTime()}.preferences_pb")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val first = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + firstJob), produceFile = { file })
            val prefs = PreferencesRepository(context, first)
            prefs.acknowledgeVersion(BuildConfig.VERSION_CODE)
            prefs.visit(Source.PAHEAL)
            firstJob.cancelAndJoin()
            val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + secondJob), produceFile = { file })
            val result = PreferencesRepository(context, reopened).flow.first()
            assertEquals(BuildConfig.VERSION_CODE, result.seenVersionCode)
            assertEquals(Source.PAHEAL, result.lastSource)
            assertTrue(result.visits.getValue(Source.PAHEAL) > 0)
            assertTrue(result.seenVersionCode != BuildConfig.VERSION_CODE + 1)
        } finally { firstJob.cancelAndJoin(); secondJob.cancelAndJoin(); file.delete() }
    }
}
