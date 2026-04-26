package com.latino

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class PelisforteProvider : MainAPI() {

    override var mainUrl            = "https://pelisforte.nu"
    override var name               = "Pelisforte"
    override val hasMainPage        = true
    override var lang               = "es"
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/peliculas"            to "🎬 Películas",
        "$mainUrl/series"               to "📺 Series",
        "$mainUrl/genero/accion"        to "💥 Acción",
        "$mainUrl/genero/comedia"       to "😂 Comedia",
        "$mainUrl/genero/drama"         to "🎭 Drama",
        "$mainUrl/genero/terror"        to "👻 Terror",
        "$mainUrl/genero/animacion"     to "🎨 Animación",
        "$mainUrl/genero/documental"    to "📽️ Documentales",
        "$mainUrl/peliculas/dobladas"   to "🌎 Dobladas al Español"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url  = if (page == 1) request.data else "${request.data}/page/$page"
        val doc  = app.get(url).document
        val items = doc.select("article.TPost, .item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title  = selectFirst("h2.Title, h3")?.text()?.trim() ?: return null
        val href   = selectFirst("a")?.attr("abs:href") ?: return null
        val poster = selectFirst("img")?.let { it.attr("data-src").ifEmpty { it.attr("src") } }
        val type   = if (href.contains("/series/")) TvType.TvSeries else TvType.Movie
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
        val isSeries = url.contains("/series/")
        return if (isSeries) {
            val eps = mutableListOf<Episode>()
            doc.select(".SeasonBx").forEachIndexed { s, season ->
                season.select("li a").forEachIndexed { e, ep ->
                    eps.add(newEpisode(ep.attr("abs:href")) {
                        this.name = ep.text()
                        this.season = s + 1
                        this.episode = e + 1
                    })
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
        doc.select("li.JFClose, li[data-option]").forEach { li ->
            val src = li.attr("data-option").ifEmpty { li.attr("data-src") }
            if (src.isNotEmpty()) loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}
