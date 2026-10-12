package com.leninasto.ruleapp.data.local

import com.google.gson.Gson
import com.leninasto.ruleapp.domain.*

data class LibraryBackup(val schema: Int = 1, val posts: List<BooruPost>, val collections: List<CollectionEntity>, val memberships: List<Membership>)
object BackupCodec {
    private val gson = Gson()
    fun encode(backup: LibraryBackup): String = gson.toJson(backup)
    fun decode(text: String): LibraryBackup {
        require(text.length <= 8 * 1024 * 1024)
        val backup = gson.fromJson(text, LibraryBackup::class.java)
        require(backup.schema == 1 && backup.posts.size <= 10_000 && backup.collections.size <= 1000 && backup.memberships.size <= 100_000)
        require(backup.collections.map { it.id }.distinct().size == backup.collections.size)
        require(backup.collections.all { it.id > 0 && it.name.isNotBlank() && it.name.length <= 80 })
        backup.posts.forEach { post ->
            val source = Source.valueOf(post.key.siteId)
            require(post.key.postId > 0 && post.tags.size <= 500 && post.tags.all { it.length <= 300 })
            require(post.publicUrl == source.postUrl(post.key.postId))
            listOf(post.previewUrl, post.sampleUrl, post.originalUrl).filterNotNull().forEach { require(resolveHttps(source.baseUrl, it) == it) }
            require(Rating.entries.contains(post.rating) && MediaKind.entries.contains(post.mediaKind))
        }
        val keys = backup.posts.map { it.key.stableId }.toSet()
        val boards = backup.collections.map { it.id }.toSet()
        require(backup.memberships.all { it.postKey in keys && it.collectionId in boards })
        return backup
    }
    suspend fun export(dao: LibraryDao): String {
        val (posts, boards, links) = dao.snapshot()
        return encode(LibraryBackup(posts = posts.map { gson.fromJson(it.json, BooruPost::class.java) }, collections = boards, memberships = links))
    }
    suspend fun restore(dao: LibraryDao, text: String) {
        val backup = decode(text)
        dao.restoreLibrary(backup.posts.map { SavedPost(it.key.stableId, gson.toJson(it)) }, backup.collections, backup.memberships)
    }
}
