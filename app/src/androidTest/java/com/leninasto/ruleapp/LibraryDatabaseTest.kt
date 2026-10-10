package com.leninasto.ruleapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leninasto.ruleapp.data.local.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryDatabaseTest {
    private lateinit var database: LibraryDatabase
    private lateinit var dao: LibraryDao
    @Before fun setup() { database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LibraryDatabase::class.java).build(); dao = database.library() }
    @After fun close() { database.close() }
    @Test fun deletingCollectionKeepsSavedPostsAndOtherMemberships() = runBlocking {
        val post = SavedPost("SHIMMIE:12", "{}")
        val a = dao.createCollection(CollectionEntity(name = "Paisajes"))
        val b = dao.createCollection(CollectionEntity(name = "Ideas"))
        dao.collect(post, a); dao.collect(post, b); dao.collect(post, a)
        assertEquals(2, dao.memberships().first().size)
        dao.deleteCollection(a)
        assertNotNull(dao.find(post.key)); assertEquals(listOf(Membership(b, post.key)), dao.memberships().first())
        dao.remove(post.key); assertTrue(dao.memberships().first().isEmpty())
    }
    @Test fun favoritesAndHistoryAreIsolatedBySource() = runBlocking {
        dao.save(SavedPost("SHIMMIE:12", "{}")); dao.save(SavedPost("DANBOORU:12", "{}"))
        dao.addHistory(SearchHistory("SHIMMIE", "landscape")); dao.addHistory(SearchHistory("DANBOORU", "landscape"))
        assertEquals(2, dao.saved().first().size); assertEquals(2, dao.history().first().size)
        dao.clearLibrary(); assertTrue(dao.saved().first().isEmpty()); assertTrue(dao.history().first().isEmpty())
    }
}
