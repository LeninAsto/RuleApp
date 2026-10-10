package com.leninasto.ruleapp.data.local

import androidx.room.*
import com.google.gson.Gson
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "saved_posts")
data class SavedPost(@PrimaryKey val key: String, val json: String, val addedAt: Long = System.currentTimeMillis())
@Entity(tableName = "collections")
data class CollectionEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "memberships", primaryKeys = ["collectionId", "postKey"],
    foreignKeys = [ForeignKey(entity = CollectionEntity::class, parentColumns = ["id"], childColumns = ["collectionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SavedPost::class, parentColumns = ["key"], childColumns = ["postKey"], onDelete = ForeignKey.CASCADE)], indices = [Index("postKey")])
data class Membership(val collectionId: Long, val postKey: String)
@Entity(tableName = "history", primaryKeys = ["source", "query"])
data class SearchHistory(val source: String, val query: String, val usedAt: Long = System.currentTimeMillis())
@Entity(tableName = "downloads")
data class DownloadEntity(@PrimaryKey val postKey: String, val downloadId: Long, val fileName: String)

@Dao
abstract class LibraryDao {
    @Query("SELECT * FROM saved_posts ORDER BY addedAt DESC") abstract fun saved(): Flow<List<SavedPost>>
    @Query("SELECT * FROM collections ORDER BY createdAt DESC") abstract fun collections(): Flow<List<CollectionEntity>>
    @Query("SELECT * FROM memberships") abstract fun memberships(): Flow<List<Membership>>
    @Query("SELECT * FROM history ORDER BY usedAt DESC LIMIT 30") abstract fun history(): Flow<List<SearchHistory>>
    @Query("SELECT * FROM downloads") abstract fun downloads(): Flow<List<DownloadEntity>>
    @Query("SELECT * FROM saved_posts WHERE `key` = :key") abstract suspend fun find(key: String): SavedPost?
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun save(post: SavedPost)
    @Query("DELETE FROM saved_posts WHERE `key` = :key") abstract suspend fun remove(key: String)
    @Insert abstract suspend fun createCollection(collection: CollectionEntity): Long
    @Query("DELETE FROM collections WHERE id = :id") abstract suspend fun deleteCollection(id: Long)
    @Insert(onConflict = OnConflictStrategy.IGNORE) abstract suspend fun addMembership(membership: Membership)
    @Query("DELETE FROM memberships WHERE collectionId = :id AND postKey = :key") abstract suspend fun removeMembership(id: Long, key: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun addHistory(history: SearchHistory)
    @Query("DELETE FROM history") abstract suspend fun clearHistory()
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun recordDownload(download: DownloadEntity)
    @Query("SELECT * FROM downloads WHERE postKey = :key") abstract suspend fun download(key: String): DownloadEntity?
    @Query("DELETE FROM downloads") abstract suspend fun clearDownloads()
    @Query("DELETE FROM saved_posts") abstract suspend fun clearSaved()
    @Query("DELETE FROM collections") abstract suspend fun clearCollections()
    @Transaction open suspend fun toggle(post: SavedPost) { if (find(post.key) == null) save(post) else remove(post.key) }
    @Transaction open suspend fun collect(post: SavedPost, collectionId: Long) { save(post); addMembership(Membership(collectionId, post.key)) }
    @Transaction open suspend fun clearLibrary() { clearCollections(); clearSaved(); clearHistory(); clearDownloads() }
}
@Database(entities = [SavedPost::class, CollectionEntity::class, Membership::class, SearchHistory::class, DownloadEntity::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() { abstract fun library(): LibraryDao }

class LibraryRepository(val dao: LibraryDao) {
    private val gson = Gson()
    val posts = dao.saved().map { list -> list.mapNotNull { runCatching { gson.fromJson(it.json, BooruPost::class.java) }.getOrNull() } }
    suspend fun toggle(post: BooruPost) = dao.toggle(entity(post))
    suspend fun collect(post: BooruPost, id: Long) = dao.collect(entity(post), id)
    private fun entity(post: BooruPost) = SavedPost(post.key.stableId, gson.toJson(post))
}
