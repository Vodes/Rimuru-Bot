package pw.vodes.rimurukt.services.rss

import com.apptasticsoftware.rssreader.RssReader
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.unbescape.html.HtmlEscape
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.ctI
import pw.vodes.rimurukt.reportException
import java.time.Instant
import java.time.temporal.ChronoUnit

val BAD_NAMING_TAGS = listOf("smol", "koala", "legion")

@Serializable
data class Feed(val name: String, val url: String, val regex: String, val serverID: String, val channelID: String) {
    var items = mutableListOf<FeedItem>()
    var firstCheck = true

    @Transient
    private var _channel: ServerTextChannel? = null

    fun channel(): ServerTextChannel {
        if (_channel == null)
            _channel = Main.api.getServerById(serverID).get().getTextChannelById(channelID).get()
        return _channel as ServerTextChannel
    }

    private fun getURL(): String {
        var feedURL = url
        if (url.contains("u2.dmhy.org", true)) {
            if (Main.config.u2Passkey.isBlank())
                return feedURL
            val match = u2PasskeyURLPattern.find(url)
            if (match != null) {
                feedURL = feedURL.replace(match.groups[1]!!.value, "passkey=${Main.config.u2Passkey}")
            }
        }
        return feedURL
    }

    fun check() {
        val reader = RssReader()
        val feedURL = getURL()
        try {
            reader.read(feedURL).forEach { item ->
                try {
                    val feedItem = FeedItem.ofItem(item)
                    val existing = items.find { it == feedItem }
                    if (existing != null)
                        return@forEach
                    if (regex.isBlank()) {
                        items.add(feedItem)
                        return@forEach
                    }
                    val matches = regex.toRegex(RegexOption.IGNORE_CASE).matches(feedItem.title)
                    if (matches)
                        items.add(feedItem)
                } catch (ex: Exception) {
                    reportException(ex, "Parsing posts for $name")
                }
            }
            var sorted = items.sortedByDescending { it.getUnixPubTime() }
            if (sorted.size > 50)
                sorted = sorted.subList(0, 49)
            items = sorted.toMutableList()
        } catch (ex: Exception) {
            reportException(ex, "Retrieving posts for $name")
        }

        postNew()
        if (firstCheck) {
            items.forEach { it.wasPosted = true }
            firstCheck = false
        }
    }

    private fun postNew() {
        val isU2 = url ctI "u2.dmhy.org"
        val isNyaa = url ctI "nyaa.si"

        for (item in items) {
            if (item.wasPosted)
                continue
            if (firstCheck && Instant.ofEpochSecond(item.getUnixPubTime()).isBefore(Instant.now().minus(24, ChronoUnit.HOURS)))
                continue

            var title = HtmlEscape.unescapeHtml(item.title)
            var badNaming = false
            for (tag in BAD_NAMING_TAGS) {
                if (title.contains("[$tag]", true) || title.contains("-$tag", true))
                    badNaming = true
            }
            if (badNaming && isNyaa) {
                title = if (title.length > 256) title.substring(0, 239) else title
                title += " (Bad Naming)"
            } else
                title = if (title.length > 256) title.substring(0, 250) else title
            val embed = EmbedBuilder().setTitle(title)

            if (isU2 || isNyaa) {
                embed.setAuthor(
                    if (isU2) "U2" else "Nyaa",
                    if (isU2) "https://u2.dmhy.org" else "https://nyaa.si",
                    if (isU2) "https://i.imgur.com/lNorPYS.png" else "https://nyaa.si/static/favicon.png"
                )
                if (isNyaa) {
                    val match = nyaaParsePattern.find(item.description)
                    if (match != null)
                        embed.setDescription("${match.groups[4]!!.value} | ${match.groups[5]!!.value}")
                }
            }

            val image = item.getImage()
            if (!image.isNullOrBlank())
                embed.setImage(image)

            embed.setUrl(item.getPostURL())
            embed.setTimestamp(Instant.ofEpochSecond(item.getUnixPubTime()))
            val future = channel().sendMessage(embed).thenAccept {
                item.wasPosted = true
                it.crossPost()
            }.exceptionally {
                reportException(it, "Could not post FeedItem with URL: ${item.getPostURL()}")
                null
            }
            // I want this to be blocking so stuff gets saved properly. Not really sure of a better way to do it.
            try {
                future.join()
            } catch (_: Exception) {
            }
        }
    }
}