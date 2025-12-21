package com.leninasto.ruleapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leninasto.ruleapp.api.ShimmieApiService
import com.leninasto.ruleapp.api.ShimmieParser
import com.leninasto.ruleapp.data.ShimmiePost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ShimmieViewModel : ViewModel() {
    private val apiService = ShimmieApiService.create()
    private val BASE_URL = "https://shimmie.shishnet.org/"
    private val ENDPOINT = "post/list"

    private val _posts = MutableStateFlow<List<ShimmiePost>>(emptyList())
    val posts: StateFlow<List<ShimmiePost>> = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        fetchPosts()
    }

    fun fetchPosts(query: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val html = apiService.getPostsHtml(search = query)
                val parsedPosts = ShimmieParser.parsePosts(html)
                
                if (parsedPosts.isEmpty()) {
                    _errorMessage.value = "No se encontraron imágenes en el HTML de: $BASE_URL$ENDPOINT. Es posible que el selector CSS haya cambiado."
                } else {
                    _posts.value = parsedPosts
                }
            } catch (e: HttpException) {
                val url = e.response()?.raw()?.request?.url?.toString() ?: "$BASE_URL$ENDPOINT"
                _errorMessage.value = "Error HTTP ${e.code()} al intentar acceder a $url"
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error al parsear HTML: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
