package com.leninasto.ruleapp.data.local

import android.content.Context
import android.os.UserManager
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** A local secret, independent of the device PIN and biometrics. Never backed up. */
class AppLock(private val context: Context, private val preferencesName: String = "app_lock", private val policy: () -> Boolean = {
    context.getSystemService(UserManager::class.java).getApplicationRestrictions(context.packageName).getBoolean("app_disabled", false)
}) {
    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _enabled = MutableStateFlow(prefs.contains("hash"))
    val enabled = _enabled.asStateFlow()
    private val _accessible = MutableStateFlow(!_enabled.value)
    val accessible = _accessible.asStateFlow()
    private val _restricted = MutableStateFlow(false)
    val restricted = _restricted.asStateFlow()
    @Volatile private var foreground = true
    init { readPolicy() }
    val pin get() = prefs.getBoolean("pin", true)
    fun lock() { if (_enabled.value) _accessible.value = false }
    fun onBackground() { foreground = false; lock() }
    fun onForeground() { foreground = true; readPolicy() }
    fun readPolicy() {
        _restricted.value = policy()
        if (_restricted.value) lock()
    }
    private fun derive(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, 210_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
    private fun verify(secret: String): Boolean {
        if (!_enabled.value) return true
        val now = System.currentTimeMillis()
        if (now < prefs.getLong("until", 0)) return false
        val valid = runCatching { MessageDigest.isEqual(derive(secret, decode(prefs.getString("salt", "")!!)), decode(prefs.getString("hash", "")!!)) }.getOrDefault(false)
        val failures = if (valid) 0 else prefs.getInt("failures", 0) + 1
        check(prefs.edit().putInt("failures", failures).putLong("until", if (failures >= 5) now + 30_000L * (1L shl (failures - 5).coerceAtMost(5)) else 0).commit())
        return valid
    }
    suspend fun unlock(secret: String): Boolean = withContext(Dispatchers.IO) { mutex.withLock {
        val valid = verify(secret)
        if (valid && !_restricted.value && foreground) _accessible.value = true
        valid && !_restricted.value && foreground
    } }
    suspend fun configure(current: String, next: String?, isPin: Boolean): Boolean = withContext(Dispatchers.IO) { mutex.withLock {
        if (!verify(current)) return@withLock false
        if (next == null) {
            check(prefs.edit().clear().commit()); _enabled.value = false; _accessible.value = true
        } else {
            require(if (isPin) next.matches(Regex("[0-9]{6,12}")) else next.length in 8..128)
            val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
            check(prefs.edit().clear().putString("salt", encode(salt)).putString("hash", encode(derive(next, salt))).putBoolean("pin", isPin).commit())
            _enabled.value = true; _accessible.value = foreground && !_restricted.value
        }
        true
    } }
}
