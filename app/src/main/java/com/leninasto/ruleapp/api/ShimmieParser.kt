package com.leninasto.ruleapp.api

import com.leninasto.ruleapp.data.ShimmiePost
import org.jsoup.Jsoup

object ShimmieParser {
    fun parsePosts(html: String): List<ShimmiePost> {
        val doc = Jsoup.parse(html)
        val posts = mutableListOf<ShimmiePost>()
        
        // En Shimmie 2, los posts suelen estar en span.thumb
        val thumbElements = doc.select("span.thumb")
        
        for (element in thumbElements) {
            try {
                val linkElement = element.selectFirst("a")
                val imgElement = element.selectFirst("img")
                
                if (linkElement != null && imgElement != null) {
                    val href = linkElement.attr("href") // Ejemplo: /post/view/6585
                    val thumbUrl = imgElement.attr("src") // Ejemplo: /data/thumbs/abc...jpg
                    val tags = imgElement.attr("title") ?: imgElement.attr("alt") ?: ""
                    
                    // Extraer ID de la URL /post/view/6585
                    val id = href.split("/").lastOrNull()?.toIntOrNull() ?: 0
                    
                    // En Shimmie, a veces podemos predecir la URL de la imagen completa
                    // Pero para ser precisos, usaremos la miniatura como base y luego 
                    // en la vista de detalle cargaremos la real si es necesario.
                    // Shishnet suele usar hashes para las imagenes reales.
                    
                    // Intentamos adivinar la extension desde el alt o title
                    val ext = when {
                        tags.contains("webm") -> "webm"
                        tags.contains("mp4") -> "mp4"
                        tags.contains("gif") -> "gif"
                        thumbUrl.endsWith(".gif") -> "gif"
                        else -> "jpg"
                    }

                    posts.add(ShimmiePost(
                        id = id,
                        imageUrl = href, // Guardamos la ruta del post para el detalle
                        thumbUrl = thumbUrl,
                        tags = tags,
                        ext = ext,
                        width = 0, // No disponible en el listado HTML simple
                        height = 0
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return posts
    }
}
