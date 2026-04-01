package pw.vodes.rimuru.services.rss

import com.apptasticsoftware.rssreader.RssReader
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import org.unbescape.html.HtmlEscape
import pw.vodes.rimuru.Main
import java.time.Instant
import java.time.temporal.ChronoUnit

@Serializable
data class RssFeed(
    val name: String,
    val url: String,
    val regex: String = "",
    val guildId: Long,
    val channelId: Long,
    val items: List<RssFeedItem> = emptyList(),
    val firstCheck: Boolean = true
) {
    fun check(u2Passkey: String): RssFeed {
        val feedUrl = getFeedUrl(u2Passkey).lightEscapeURL()
        val reader = RssReader()
        val updatedItems = items.toMutableList()

        try {
            reader.read(feedUrl).forEach { item ->
                try {
                    val feedItem = RssFeedItem.from(item)
                    val exists = updatedItems.any { existing -> existing.matches(feedItem) }
                    if (exists) {
                        return@forEach
                    }

                    if (regex.isBlank()) {
                        updatedItems.add(feedItem)
                        return@forEach
                    }

                    val matches = regex.toRegex(RegexOption.IGNORE_CASE).matches(feedItem.title)
                    if (matches) {
                        updatedItems.add(feedItem)
                    }
                } catch (exception: Exception) {
                    reportRssException(exception, "Parsing posts for $name")
                }
            }
        } catch (exception: Exception) {
            reportRssException(exception, "Retrieving posts for $name")
        }

        val trimmedItems = updatedItems
            .sortedByDescending { it.getUnixPubTime() }
            .take(50)
            .toMutableList()

        val channelResolved = postNew(trimmedItems)
        if (!channelResolved) {
            return copy(items = trimmedItems.toList(), firstCheck = firstCheck)
        }

        return if (firstCheck) {
            copy(
                items = trimmedItems.map { it.copy(wasPosted = true) },
                firstCheck = false
            )
        } else {
            copy(items = trimmedItems.toList(), firstCheck = false)
        }
    }

    private fun getFeedUrl(u2Passkey: String): String {
        var feedUrl = url.trim()
        if (!url.contains("u2.dmhy.org", ignoreCase = true) || u2Passkey.isBlank()) {
            return feedUrl
        }

        val match = u2PasskeyUrlPattern.find(url) ?: return feedUrl
        return feedUrl.replace(match.groups[1]!!.value, "passkey=$u2Passkey")
    }

    private fun postNew(updatedItems: MutableList<RssFeedItem>): Boolean {
        val channel = resolveChannel() ?: run {
            reportRssException(IllegalStateException("Channel $channelId not found in guild $guildId"), "Could not resolve channel for RSS feed $name")
            return false
        }
        val isU2 = url.contains("u2.dmhy.org", ignoreCase = true)
        val isNyaa = url.contains("nyaa.si", ignoreCase = true)

        for ((index, item) in updatedItems.withIndex()) {
            if (item.wasPosted) {
                continue
            }
            if (firstCheck && Instant.ofEpochSecond(item.getUnixPubTime())
                    .isBefore(Instant.now().minus(24, ChronoUnit.HOURS))
            ) {
                continue
            }

            val embed = buildEmbed(item, isU2, isNyaa)
            try {
                val message = channel.sendMessageEmbeds(embed.build()).complete()
                if (channel is NewsChannel) {
                    message.crosspost().queue()
                }
                updatedItems[index] = item.copy(wasPosted = true)
            } catch (exception: Exception) {
                reportRssException(exception, "Could not post RSS item with URL: ${item.getPostUrl()}")
            }
        }

        return true
    }

    private fun buildEmbed(item: RssFeedItem, isU2: Boolean, isNyaa: Boolean): EmbedBuilder {
        val title = HtmlEscape.unescapeHtml(item.title).let {
            if (it.length > 256) it.substring(0, 250) else it
        }

        return EmbedBuilder()
            .setTitle(title)
            .setUrl(item.getPostUrl())
            .setTimestamp(Instant.ofEpochSecond(item.getUnixPubTime()))
            .apply {
                if (isU2 || isNyaa) {
                    setAuthor(
                        if (isU2) "U2" else "Nyaa",
                        if (isU2) "https://u2.dmhy.org" else "https://nyaa.si",
                        if (isU2) "https://i.imgur.com/lNorPYS.png" else "https://nyaa.si/static/favicon.png"
                    )
                }

                if (isNyaa) {
                    val match = nyaaParsePattern.find(item.description)
                    if (match != null) {
                        setDescription("${match.groups[4]!!.value} | ${match.groups[5]!!.value}")
                    }
                }

                item.getImage()?.takeIf { it.isNotBlank() }?.let(::setImage)
            }
    }

    private fun resolveChannel(): GuildMessageChannel? {
        val guild = Main.jda.getGuildById(guildId) ?: return null
        return guild.getTextChannelById(channelId)
            ?: guild.getNewsChannelById(channelId)
    }
}

private fun String.lightEscapeURL(): String {
    return replace("\"", "%22")
        .replace(" ", "%20")
        .replace("[", "%5B")
        .replace("]", "%5D")
        .replace("|", "%7C")
}

internal fun reportRssException(exception: Throwable, context: String) {
    System.err.println("[rss] $context")
    exception.printStackTrace()
}
