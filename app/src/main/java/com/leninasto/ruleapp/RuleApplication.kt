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
    override fun onCreate() {
        super.onCreate()
        com.leninasto.ruleapp.core.AppStrings.initialize(this)
        val foreground = com.leninasto.ruleapp.domain.ForegroundTracker({ container.lock.onForeground() }, { container.lock.onBackground() })
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: android.app.Activity) = foreground.started()
            override fun onActivityStopped(activity: android.app.Activity) = foreground.stopped(activity.isChangingConfigurations)
            override fun onActivityCreated(activity: android.app.Activity, state: android.os.Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, state: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }
    override fun onConfigurationChanged(config: android.content.res.Configuration) {
        super.onConfigurationChanged(config)
        com.leninasto.ruleapp.core.AppStrings.invalidate()
    }
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
    val updates = com.leninasto.ruleapp.data.remote.UpdateChecker()
}
