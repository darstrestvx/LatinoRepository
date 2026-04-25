package com.latino

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class PeliculasYonkisProvider : MainAPI() {

    override var mainUrl            = "https://www.peliculasyonkis.com"
    override var name               = "PeliculasYonkis"
    override val hasMainPage        = true
    override var lang               = "es"
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/peliculas"         to "🎬 Películas",
        "$mainUrl/genero/accion"     to "💥 Acción",
        "$mainUrl/genero/aventura"   to "🗺️ Aventura",
        "$mainUrl/genero/animacion"  to "🎨 Animación",
        "$mainUrl/genero/comedia"    to "😂 Comedia",
        "$mainUrl/genero/drama"      to "🎭 Drama",
        "$mainUrl/genero/romance"    to "❤️ Romance",
        "$mainUrl/genero/terror"     to "👻 Terror",
        "$mainUrl/genero/western"    to "🤠 Western"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url  = if (page == 1) request.data else "${request.data}/page/$page/"
        val doc  = app.get(url).document
        val items = doc.select(".movie-item, article").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title  = selectFirst("h2, h3, .title")?.text()?.trim() ?: return null
        val href   = selectFirst("a")?.attr("abs:href") ?: return null
        val poster = selectFirst("img")?.attr("src")
        return newMovieSearchResponse(title, href, TvType.Movie) { posterUrl = poster }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return app.get("$mainUrl/?s=$query").document
            .select(".movie-item, article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc    = app.get(url).document
        val title  = doc.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = doc.selectFirst(".poster img")?.attr("src")
        val desc   = doc.selectFirst(".description p")?.text()?.trim()
        val year   = doc.selectFirst(".year, .fecha")?.text()?.trim()?.toIntOrNull()
        val tags   = doc.select(".genres a").map { it.text() }
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            posterUrl = poster; plot = desc; this.year = year; this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("iframe, .player-option").forEach { el ->
            val src = el.attr("src").ifEmpty { el.attr("data-src") }
            if (src.isNotEmpty()) loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}
