package com.leninasto.ruleapp

import android.graphics.*
import com.leninasto.ruleapp.domain.*
import kotlinx.coroutines.runBlocking

/** Synthetic, non-sensitive media. This class is packaged only in the benchmark APK. */
class BenchmarkApplication : RuleApplication() {
    override val container by lazy { AppContainer(this, FixtureRepository()) }
    override fun onCreate() {
        super.onCreate()
        runBlocking {
            container.preferences.confirm(setOf(Rating.GENERAL))
            container.preferences.visit(Source.SHIMMIE)
            container.preferences.acknowledgeVersion(BuildConfig.VERSION_CODE)
        }
    }
    private inner class FixtureRepository : BooruRepository {
        private val previews by lazy {
            (0..7).map { i ->
                val file = cacheDir.resolve("landscape_$i.jpg")
                if (!file.exists()) {
                    val bitmap = Bitmap.createBitmap(360, 420 + i * 45, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    canvas.drawColor(Color.rgb(180 + i * 7, 185, 220))
                    paint.color = Color.rgb(100, 85 + i * 9, 155)
                    val mountain = Path().apply { moveTo(0f, bitmap.height.toFloat()); lineTo(180f, 150f + i * 20); lineTo(360f, bitmap.height.toFloat()); close() }
                    canvas.drawPath(mountain, paint)
                    paint.color = Color.rgb(250, 226, 160); canvas.drawCircle(270f, 75f, 35f, paint)
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }; bitmap.recycle()
                }
                file.toURI().toString()
            }
        }
        override suspend fun posts(spec: SearchSpec, page: PageKey, limit: Int): PostPage {
            val number = (page as? PageKey.Number)?.value ?: 1
            return PostPage((0 until 30).map { n ->
                val id = (number - 1) * 30L + n + 1
                BooruPost(PostKey(spec.source.name, id), previews[n % 8], previews[n % 8], previews[n % 8], 360, 420 + (n % 8) * 45,
                    listOf("landscape", "synthetic_fixture"), Rating.GENERAL, "jpg", MediaKind.IMAGE, spec.source.postUrl(id))
            }, if (number < 20) PageKey.Number(number + 1) else null)
        }
        override suspend fun post(source: Source, id: Long): BooruPost? = null
        override suspend fun suggestions(source: Source, prefix: String) = emptyList<TagSuggestion>()
    }
}
