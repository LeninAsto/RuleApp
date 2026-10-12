package com.leninasto.ruleapp.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.settingsStore by preferencesDataStore("settings")
enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Oscuro") }
data class AppPreferences(val adultConfirmed: Boolean = false, val ratings: Set<Rating> = emptySet(), val theme: ThemeMode = ThemeMode.SYSTEM, val dynamicColor: Boolean = false,
    val seenVersionCode: Int = 0, val lastSource: Source = Source.SHIMMIE, val visits: Map<Source, Long> = emptyMap(), val compactGallery: Boolean = true, val lastUpdateCheck: Long = 0, val sourceAccent: Boolean = false)
class PreferencesRepository(context: Context, private val store: androidx.datastore.core.DataStore<Preferences> = context.settingsStore) {
    private val adult = booleanPreferencesKey("adult_confirmed")
    private val ratings = stringSetPreferencesKey("visible_ratings")
    private val theme = stringPreferencesKey("theme")
    private val dynamic = booleanPreferencesKey("dynamic_color")
    private val seenVersion = intPreferencesKey("seen_version")
    private val lastSource = stringPreferencesKey("last_source")
    private val compact = booleanPreferencesKey("compact_gallery")
    private val updateCheck = longPreferencesKey("update_check")
    private val sourceAccent = booleanPreferencesKey("source_accent")
    val flow = store.data.map { p -> AppPreferences(p[adult] ?: false,
        p[ratings].orEmpty().mapNotNull { runCatching { Rating.valueOf(it) }.getOrNull() }.toSet(),
        runCatching { ThemeMode.valueOf(p[theme].orEmpty()) }.getOrDefault(ThemeMode.SYSTEM), p[dynamic] ?: false,
        p[seenVersion] ?: 0, runCatching { Source.valueOf(p[lastSource].orEmpty()) }.getOrDefault(Source.SHIMMIE),
        Source.entries.mapNotNull { source -> p[longPreferencesKey("visited_${source.name}")]?.let { source to it } }.toMap(), p[compact] ?: true, p[updateCheck] ?: 0, p[sourceAccent] ?: false) }
    suspend fun confirm(selected: Set<Rating>) = store.edit { it[adult] = true; it[ratings] = selected.map(Rating::name).toSet() }
    suspend fun ratings(selected: Set<Rating>) = store.edit { it[ratings] = selected.map(Rating::name).toSet() }
    suspend fun theme(mode: ThemeMode) = store.edit { it[theme] = mode.name }
    suspend fun dynamic(enabled: Boolean) = store.edit { it[dynamic] = enabled }
    suspend fun acknowledgeVersion(code: Int) = store.edit { it[seenVersion] = code }
    suspend fun visit(source: Source) = store.edit { it[lastSource] = source.name; it[longPreferencesKey("visited_${source.name}")] = System.currentTimeMillis() }
    suspend fun compact(enabled: Boolean) = store.edit { it[compact] = enabled }
    suspend fun markUpdateCheck() = store.edit { it[updateCheck] = System.currentTimeMillis() }
    suspend fun sourceAccent(enabled: Boolean) = store.edit { it[sourceAccent] = enabled }
}

/** Only AES-GCM ciphertext is persisted. Never stored in ordinary DataStore or backups. */
class CredentialStore(context: Context, preferencesName: String = "credentials", private val alias: String = "ruleapp.credentials.v1") {
    val revision = MutableStateFlow(0)
    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun get(source: Source): Credentials? = runCatching {
        val encoded = prefs.getString(source.name, null) ?: return null
        val parts = encoded.split(':')
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        cipher.updateAAD(source.name.toByteArray())
        val plain = String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8).split('\n', limit = 2)
        Credentials(plain[0], plain[1])
    }.getOrNull()
    suspend fun put(source: Source, value: Credentials) = withContext(Dispatchers.IO) {
        require(value.user.isNotBlank() && value.apiKey.isNotBlank() && !value.user.contains('\n') && !value.apiKey.contains('\n'))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key()); cipher.updateAAD(source.name.toByteArray())
        val encrypted = cipher.doFinal("${value.user.trim()}\n${value.apiKey.trim()}".toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString(source.name, Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
        revision.value += 1
    }
    suspend fun clear(source: Source? = null) = withContext(Dispatchers.IO) {
        val edit = prefs.edit(); if (source == null) edit.clear() else edit.remove(source.name); check(edit.commit())
        revision.value += 1
    }
}
