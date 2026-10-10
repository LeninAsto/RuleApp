package com.leninasto.ruleapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.leninasto.ruleapp.data.local.AppLock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AppLockTest {
    @Test fun managedRestrictionCannotBeUnlockedWithTheLocalSecret() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_lock_qa_test", Context.MODE_PRIVATE).edit().clear().commit()
        var restricted = true
        val lock = AppLock(context, "app_lock_qa_test") { restricted }
        assertTrue(lock.restricted.value); assertFalse(lock.unlock(""))
        restricted = false; lock.readPolicy()
        assertFalse(lock.restricted.value); assertTrue(lock.unlock(""))
    }
    @Test fun localPinPersistsLocksAndCannotBeRemovedWithoutCurrentSecret() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("app_lock_qa_test", Context.MODE_PRIVATE).edit().clear().commit()
        val lock = AppLock(context, "app_lock_qa_test")
        try {
            assertTrue(lock.configure("", "654321", true)); lock.lock()
            assertFalse(lock.accessible.value)
            assertFalse(lock.unlock("000000")); assertFalse(lock.configure("000000", null, true))
            assertTrue(lock.unlock("654321")); assertTrue(lock.accessible.value)
            lock.onBackground(); assertFalse(lock.unlock("654321"))
            lock.onForeground(); assertFalse(lock.accessible.value)
            assertTrue(lock.unlock("654321"))
            val recreated = AppLock(context, "app_lock_qa_test")
            assertFalse(recreated.accessible.value)
            assertTrue(recreated.configure("654321", "secret phrase", false))
            assertTrue(recreated.configure("secret phrase", null, false))
            assertFalse(context.getSharedPreferences("app_lock_qa_test", Context.MODE_PRIVATE).all.toString().contains("654321"))
        } finally { context.getSharedPreferences("app_lock_qa_test", Context.MODE_PRIVATE).edit().clear().commit() }
    }
}
