package pw.vodes.rimuru.config

import kotlinx.serialization.Serializable
import pw.vodes.rimuru.services.rss.RssFeed

@Serializable
data class RssConfig(
    val feeds: List<RssFeed> = emptyList()
)
