package com.latino

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class CineCalidadProvider : MainAPI() {

    override var mainUrl            = "https://cinecalidad.lol"
    override var name               = "CineCalidad"
    override val hasMainPage        = true
    override var lang               = "es"
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/ver-peliculas"             to "🎬 Películas 4K/HD",
        "$mainUrl/ver-series"                to "📺 Series HD",
        "$mainUrl/genero/accion-y-aventuras" to "💥 Acción y Aventuras",
        "$mainUrl/genero/animacion"          to "🎨 Animación",
        "$mainUrl/genero/ciencia-ficcion"    to "🚀 Ciencia Ficción",
        "$mainUrl/genero/comedia"            to "😂 Comedia",
        "$mainUrl/genero/crimen"             to "🔍 Crimen",
        "$mainUrl/genero/documental"         to "📽️ Documental",
        "$mainUrl/genero/fantasia"           to "🧙 Fantasía",
        "$mainUrl/genero/romance"            to "❤️ Romance",
        "$mainUrl/genero/suspenso"           to "😰 Suspenso"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url  = if (page == 1) request.data else "${request.data}/page/$page/"
        val doc  = app.get(url).document
        val items = doc.select("article.TPost").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title  = selectFirst("h2.Title, h3")?.text()?.trim() ?: return null
        val href   = selectFirst("a")?.attr("abs:href") ?: return null
        val poster = selectFirst("img.lazy, img")?.let { it.attr("data-src").ifEmpty { it.attr("src") } }
        val type   = if (href.contains("/ver-serie/")) TvType.TvSeries else TvType.Movie
        return newMovieSearchResponse(title, href, type) { posterUrl = poster }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return app.get("$mainUrl/?s=$query").document
            .select("article.TPost").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc    = app.get(url).document
        val title  = doc.selectFirst("h1.Title")?.text()?.trim() ?: return null
        val poster = doc.selectFirst("div.Image img")?.attr("src")
        val desc   = doc.selectFirst("div.Description p")?.text()?.trim()
        val year   = doc.selectFirst("span.Date")?.text()?.trim()?.toIntOrNull()
        val tags   = doc.select("p.Genre a").map { it.text() }
        val isSeries = url.contains("/ver-serie/")
        return if (isSeries) {
            val eps = mutableListOf<Episode>()
            doc.select(".se-c").forEachIndexed { sIdx, season ->
                season.select("li a").forEachIndexed { eIdx, ep ->
                    eps.add(Episode(ep.attr("abs:href"), ep.text(), sIdx + 1, eIdx + 1))
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, eps) {
                posterUrl = poster; plot = desc; this.year = year; this.tags = tags
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                posterUrl = poster; plot = desc; this.year = year; this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("track[kind=subtitles]").forEach { t ->
            val src = t.attr("src")
            if (src.isNotEmpty()) subtitleCallback(SubtitleFile("es", src))
        }
        doc.select(".dooplay_player_option, li[data-option]").forEach { opt ->
            val src = opt.attr("data-option").ifEmpty { opt.attr("value") }
            if (src.isNotEmpty()) loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}
