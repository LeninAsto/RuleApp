package com.leninasto.ruleapp

import android.app.Application
import androidx.room.Room
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.leninasto.ruleapp.data.local.*
import com.leninasto.ruleapp.data.remote.NetworkRepository
import com.leninasto.ruleapp.domain.BooruRepository
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

open class RuleApplication : Application(), SingletonImageLoader.Factory {
    open val container by lazy { AppContainer(this) }
    override fun newImageLoader(context: android.content.Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { OkHttpClient.Builder().build() }))
            if (android.os.Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
        }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images").toOkioPath()).maxSizeBytes(256L * 1024 * 1024).build() }
        .build()
}
class AppContainer(app: Application, repositoryOverride: BooruRepository? = null, databaseName: String = "ruleapp.db") {
    val lock = AppLock(app)
    val credentials = CredentialStore(app)
    val preferences = PreferencesRepository(app)
    val database = Room.databaseBuilder(app, LibraryDatabase::class.java, databaseName).build()
    val library = LibraryRepository(database.library())
    val repository = repositoryOverride ?: NetworkRepository(credentials::get)
    val downloads = Downloads(app, database.library())
}
