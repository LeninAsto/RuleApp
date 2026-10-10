package com.leninasto.ruleapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leninasto.ruleapp.data.local.CredentialStore
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialStoreTest {
    @Test fun secretsAreEncryptedAndIsolated() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CredentialStore(context, "credentials_qa_test", "ruleapp.credentials.qa-test")
        try {
            store.put(Source.RULE34, Credentials("123", "instrumentation-only-key"))
            assertEquals("instrumentation-only-key", store.get(Source.RULE34)?.apiKey)
            assertNull(store.get(Source.DANBOORU))
            val raw = context.getSharedPreferences("credentials_qa_test", Context.MODE_PRIVATE).getString(Source.RULE34.name, "")!!
            assertFalse(raw.contains("instrumentation-only-key")); assertNotEquals("123\ninstrumentation-only-key", raw)
            store.clear(Source.RULE34); assertNull(store.get(Source.RULE34))
        } finally { store.clear() }
    }
}
