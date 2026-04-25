package com.latino

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class PelisPlusPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(PelisPlusProvider())
    }
}

@CloudstreamPlugin
class CuevanaPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CuevanaProvider())
    }
}

@CloudstreamPlugin
class RepelisPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(RepelisProvider())
    }
}

@CloudstreamPlugin
class CineCalidadPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CineCalidadProvider())
    }
}

@CloudstreamPlugin
class SeriesFlixPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(SeriesFlixProvider())
    }
}

@CloudstreamPlugin
class DoramasFlixPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(DoramasFlixProvider())
    }
}

@CloudstreamPlugin
class PeliculasYonkisPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(PeliculasYonkisProvider())
    }
}

@CloudstreamPlugin
class PelisfortePlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(PelisforteProvider())
    }
}
