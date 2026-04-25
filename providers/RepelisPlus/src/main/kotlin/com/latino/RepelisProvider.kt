package com.latino

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class RepelisProvider : MainAPI() {

    override var mainUrl            = "https://repelis.net"
    override var name               = "Repelis"
    override val hasMainPage        = true
    override var lang               = "es"
    override val hasDownloadSupport = true
    override val supportedTypes     = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/peliculas"                 to "🎬 Películas HD",
        "$mainUrl/series"                    to "📺 Series HD",
        "$mainUrl/peliculas/genero/accion"   to "💥 Acción",
        "$mainUrl/peliculas/genero/thriller" to "🔪 Thriller",
        "$mainUrl/peliculas/genero/terror"   to "👻 Terror",
        "$mainUrl/peliculas/genero/comedia"  to "😂 Comedia",
        "$mainUrl/peliculas/genero/drama"    to "🎭 Drama",
        "$mainUrl/peliculas/genero/familiar" to "👨‍👩‍👧 Familiar",
        "$mainUrl/estrenos"                  to "⭐ Últimos Estrenos"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url  = if (page == 1) request.data else "${request.data}/page/$page"
        val doc  = app.get(url).document
        val items = doc.select(".item, article.TPost").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title  = selectFirst("h2, .Title, h3.name")?.text()?.trim() ?: return null
        val href   = selectFirst("a")?.attr("abs:href") ?: return null
        val poster = selectFirst("img")?.let { it.attr("data-src").ifEmpty { it.attr("src") } }
        val type   = if (href.contains("/serie")) TvType.TvSeries else TvType.Movie
        return newMovieSearchResponse(title, href, type) { posterUrl = poster }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return app.get("$mainUrl/buscar/?s=$query").document
            .select(".item, article.TPost").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc    = app.get(url).document
        val title  = doc.selectFirst("h1.Title, h1")?.text()?.trim() ?: return null
        val poster = doc.selectFirst(".Image img")?.attr("src")
        val desc   = doc.selectFirst(".Description p")?.text()?.trim()
        val year   = doc.selectFirst(".Date, .year")?.text()?.trim()?.toIntOrNull()
        val tags   = doc.select(".Genre a").map { it.text() }
        val isSeries = url.contains("/serie")
        return if (isSeries) {
            val eps = mutableListOf<Episode>()
            doc.select(".SeasonBx").forEachIndexed { s, season ->
                season.select(".EpsList li a").forEachIndexed { e, ep ->
                    eps.add(Episode(ep.attr("abs:href"), ep.text(), s + 1, e + 1))
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
        doc.select("ul.TPlayerNv li, .server-option").forEach { server ->
            val src = server.attr("data-option").ifEmpty { server.attr("data-src") }
            if (src.isNotEmpty()) loadExtractor(src, data, subtitleCallback, callback)
        }
        return true
    }
}
