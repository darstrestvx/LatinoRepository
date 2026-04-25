package com.latino

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class SeriesFlixProvider : MainAPI() {

    override var mainUrl            = "https://seriesflix.video"
    override var name               = "SeriesFlix"
    override val hasMainPage        = true
    override var lang               = "es"
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/series"                 to "📺 Todas las Series",
        "$mainUrl/genero/accion"          to "💥 Acción",
        "$mainUrl/genero/comedia"         to "😂 Comedia",
        "$mainUrl/genero/drama"           to "🎭 Drama",
        "$mainUrl/genero/ciencia-ficcion" to "🚀 Ciencia Ficción",
        "$mainUrl/genero/crimen"          to "🔍 Crimen",
        "$mainUrl/genero/fantasia"        to "🧙 Fantasía",
        "$mainUrl/genero/terror"          to "👻 Terror",
        "$mainUrl/estrenos"               to "⭐ Estrenos Series"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url   = if (page == 1) request.data else "${request.data}/page/$page"
        val doc   = app.get(url).document
        val items = doc.select(".MovieList article").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title  = selectFirst("h2.Title, h3")?.text()?.trim() ?: return null
        val href   = selectFirst("a")?.attr("abs:href") ?: return null
        val poster = selectFirst("img")?.let { it.attr("data-src").ifEmpty { it.attr("src") } }
        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { posterUrl = poster }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return app.get("$mainUrl/?s=$query").document
            .select(".MovieList article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc    = app.get(url).document
        val title  = doc.selectFirst("h1.Title, h1")?.text()?.trim() ?: return null
        val poster = doc.selectFirst("div.Image img")?.attr("src")
        val desc   = doc.selectFirst("div.Description p")?.text()?.trim()
        val year   = doc.selectFirst("span.Date")?.text()?.trim()?.toIntOrNull()
        val tags   = doc.select("p.Genre a").map { it.text() }
        val eps    = mutableListOf<Episode>()
        doc.select(".se-c").forEachIndexed { sIdx, season ->
            season.select("li a").forEachIndexed { eIdx, ep ->
                eps.add(Episode(
                    data    = ep.attr("abs:href"),
                    name    = ep.selectFirst(".epst")?.text() ?: "Episodio ${eIdx + 1}",
                    season  = sIdx + 1,
                    episode = eIdx + 1
                ))
            }
        }
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, eps) {
            posterUrl = poster; plot = desc; this.year = year; this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select(".dooplay_player_option, li.JFClose").forEach { opt ->
            val src = opt.attr("data-option").ifEmpty { opt.attr("data-src") }
            if (src.isNotEmpty()) loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}
