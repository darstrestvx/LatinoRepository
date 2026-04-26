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
