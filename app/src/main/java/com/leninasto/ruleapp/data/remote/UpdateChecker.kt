package com.leninasto.ruleapp.data.remote
import com.leninasto.ruleapp.R

import com.leninasto.ruleapp.BuildConfig
import com.leninasto.ruleapp.domain.AppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class UpdateState(val checking: Boolean = false, val latest: String? = null, val available: Boolean = false, val message: Int? = null)
class UpdateChecker {
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(12, TimeUnit.SECONDS).build()
    private val mutable = MutableStateFlow(UpdateState())
    val state = mutable.asStateFlow()
    suspend fun check() = withContext(Dispatchers.IO) {
        mutable.value = UpdateState(checking = true)
        try {
            client.newCall(Request.Builder().url("https://raw.githubusercontent.com/LeninAsto/RuleApp/master/ver.txt").build()).execute().use { response ->
                if (!response.isSuccessful) { mutable.value = UpdateState(message = if (response.code == 404) R.string.ui_the_version_file_has_not_been_published_in else R.string.ui_could_not_check_the_version_try_again_later); return@withContext }
                val raw = response.body?.byteStream()?.use { it.readBytesLimited() } ?: error("Missing version")
                val version = AppVersion.parse(raw) ?: error("Invalid version")
                val installed = AppVersion.parse(BuildConfig.VERSION_NAME) ?: error("Invalid installed version")
                mutable.value = UpdateState(latest = raw.trim(), available = version > installed, message = if (version > installed) R.string.ui_a_new_version_is_available else R.string.ui_you_have_the_latest_version)
            }
        } catch (e: CancellationException) { mutable.value = UpdateState(); throw e }
        catch (_: Exception) { mutable.value = UpdateState(message = R.string.ui_could_not_check_for_updates_check_your_connection) }
    }
    private fun java.io.InputStream.readBytesLimited(): String {
        val result = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(256)
        while (true) { val count = read(buffer); if (count < 0) break; check(result.size() + count <= 1024); result.write(buffer, 0, count) }
        return result.toString("UTF-8")
    }
}
