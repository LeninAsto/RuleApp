package com.leninasto.ruleapp

import com.leninasto.ruleapp.data.local.*
import com.leninasto.ruleapp.domain.*
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    private val post = BooruPost(PostKey("SHIMMIE", 1), null, null, "https://shimmie.shishnet.org/file.jpg", 1, 1, listOf("landscape"), Rating.UNKNOWN, "jpg", MediaKind.IMAGE, Source.SHIMMIE.postUrl(1))
    @Test fun preservesMultipleCollectionsWithoutCredentials() {
        val backup = LibraryBackup(posts = listOf(post), collections = listOf(CollectionEntity(1, "Uno"), CollectionEntity(2, "Dos")), memberships = listOf(Membership(1, post.key.stableId), Membership(2, post.key.stableId)))
        val json = BackupCodec.encode(backup)
        assertFalse(json.contains("apiKey")); assertFalse(json.contains("password"))
        assertEquals(backup, BackupCodec.decode(json))
    }
    @Test fun rejectsDanglingMembershipAndUnsafeMediaUrlBeforeImport() {
        val invalid = LibraryBackup(posts = listOf(post), collections = emptyList(), memberships = listOf(Membership(999, post.key.stableId)))
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(invalid)) }
        val insecure = invalid.copy(posts = listOf(post.copy(originalUrl = "http://example.com/file.jpg")), memberships = emptyList())
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(BackupCodec.encode(insecure)) }
    }
}
