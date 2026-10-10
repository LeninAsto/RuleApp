package com.leninasto.ruleapp

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leninasto.ruleapp.domain.*
import com.leninasto.ruleapp.ui.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class NativeAppTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var container: AppContainer
    private lateinit var model: AppViewModel
    private val store = ViewModelStore()
    private val calls = CopyOnWriteArrayList<SearchSpec>()
    private var failAppend = false
    private val repository = object : BooruRepository {
        override suspend fun posts(spec: SearchSpec, page: PageKey, limit: Int): PostPage {
            calls += spec
            if (spec.query == "slow") delay(10_000)
            if (page == PageKey.Number(2) && failAppend) throw BooruException(ErrorKind.CONNECTION)
            val start = if (page == PageKey.Number(2)) 31L else 1L
            return PostPage((start until start + 30).map { id -> BooruPost(PostKey(spec.source.name, id), null, null, null, 600, if (id % 2 == 0L) 800 else 500, listOf("landscape"), Rating.GENERAL, "jpg", MediaKind.IMAGE, spec.source.postUrl(id)) }, if (start == 1L) PageKey.Number(2) else null)
        }
        override suspend fun post(source: Source, id: Long): BooruPost? = null
        override suspend fun suggestions(source: Source, prefix: String) = emptyList<TagSuggestion>()
    }
    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        container = AppContainer(app, repository, "ui-test.db")
        runBlocking { container.library.dao.clearLibrary(); container.preferences.confirm(setOf(Rating.GENERAL)) }
        compose.runOnUiThread { model = AppViewModel(container, SavedStateHandle()); store.put("test", model) }
        compose.setContent { NativeApp(model) }
        compose.onNodeWithText("Continuar a RuleApp").performClick()
        compose.waitUntil(10_000) { calls.isNotEmpty() }
    }
    @After fun cleanup() { compose.runOnUiThread { store.clear() }; container.database.close() }
    @Test fun favoritesCollectionsAndNavigationWork() {
        compose.onAllNodesWithContentDescription("Guardar en favoritos").onFirst().performClick()
        compose.waitUntil(5000) { model.saved.value.isNotEmpty() }
        compose.onNodeWithText("Guardados").performClick()
        compose.onNodeWithText("Tus guardados").assertIsDisplayed()
        compose.onNodeWithContentDescription("Crear colecciÃ³n").performClick()
        compose.onNodeWithText("Nombre").performTextInput("Paisajes")
        compose.onNodeWithText("Crear", useUnmergedTree = true).performClick()
        compose.waitUntil(5000) { model.collections.value.any { it.name == "Paisajes" } }
        compose.onNodeWithText("Paisajes").assertIsDisplayed()
        compose.onNodeWithText("Info", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Abrir Ajustes").assertIsDisplayed()
    }
    @Test fun sourceSwitchCancelsOldSearchAndRepeatingQueryReloads() {
        compose.runOnUiThread { model.search("slow") }
        compose.waitUntil(5000) { calls.any { it.query == "slow" } }
        compose.runOnUiThread { model.source(Source.DANBOORU); model.search("landscape") }
        compose.waitUntil(5000) { calls.any { it.source == Source.DANBOORU && it.query == "landscape" } }
        compose.waitUntil(5000) { model.spec.value.source == Source.DANBOORU }
        val previous = calls.count { it.query == "landscape" }
        compose.runOnUiThread { model.search("landscape") }
        compose.waitUntil(5000) { calls.count { it.query == "landscape" } > previous }
        assertEquals(Source.DANBOORU, model.spec.value.source)
    }
    @Test fun detailOpensAndReturnsToGallery() {
        compose.onAllNodesWithContentDescription("PublicaciÃ³n 1, Imagen, General").onFirst().performClick()
        compose.onNodeWithText("#1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver a la galerÃ­a").performClick()
        compose.onAllNodesWithText("Explorar", useUnmergedTree = true).onFirst().assertExists()
    }
    @Test fun appendFailureCanRetryWithoutResettingGallery() {
        failAppend = true
        compose.onNodeWithTag("gallery").performScrollToIndex(29)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("No pudimos cargar mÃ¡s").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("gallery").performScrollToIndex(30)
        compose.onNodeWithText("No pudimos cargar mÃ¡s").assertExists()
        failAppend = false
        compose.onNodeWithText("Reintentar").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("No pudimos cargar mÃ¡s").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("gallery").performScrollToIndex(31)
        compose.onNodeWithTag("gallery").performScrollToIndex(0)
        compose.onAllNodesWithContentDescription("PublicaciÃ³n 1, Imagen, General").onFirst().assertIsDisplayed()
    }
    @Test fun switchingSourceRestoresScrollAndDoesNotReloadCachedPage() {
        compose.onNodeWithTag("gallery").performScrollToIndex(14)
        val count = calls.count { it.source == Source.SHIMMIE }
        compose.runOnUiThread { model.source(Source.PAHEAL) }
        compose.waitUntil(5000) { calls.any { it.source == Source.PAHEAL } }
        compose.runOnUiThread { model.source(Source.SHIMMIE) }
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription("PublicaciÃ³n 15, Imagen, General").onFirst().assertIsDisplayed()
        assertEquals(count, calls.count { it.source == Source.SHIMMIE })
    }
}
