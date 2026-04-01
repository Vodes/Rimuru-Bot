package pw.vodes.rimuru.services.rss

import com.apptasticsoftware.rssreader.Item
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val imageUrlPattern =
    "[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\-\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/=]*)(.jpg|.png|.jpeg|.gif|.webp|.avif)".toRegex(RegexOption.IGNORE_CASE)
private val u2AttachImagePattern = "^attachments\\/\\d{6}\\/.*".toRegex(RegexOption.IGNORE_CASE)
internal val u2PasskeyUrlPattern =
    "https?:\\/\\/u2\\.dmhy\\.org\\/torrentrss\\.php.*(passkey=[^& ]*).*".toRegex(RegexOption.IGNORE_CASE)
internal val nyaaParsePattern =
    "(<.*>)(#\\d*) \\| (.+)(?:<\\/a>) \\| (\\d*\\.\\d* (?:GiB|MiB|TiB)) \\| ([^|]*) \\| (.*)".toRegex(RegexOption.IGNORE_CASE)

@Serializable
data class RssFeedItem(
    val title: String,
    val author: String,
    val category: String,
    val description: String,
    val guid: String,
    val link: String,
    val pubDate: String,
    val wasPosted: Boolean = false
) {
    fun getImage(): String? {
        if (description.isBlank()) {
            return null
        }

        val match = imageUrlPattern.find(description) ?: return null
        var url = match.groups[0]!!.value
        url = when {
            u2AttachImagePattern.matches(url) -> "https://u2.dmhy.org/$url"
            url.startsWith("//") -> "https:$url"
            else -> url
        }

        return url
    }

    fun getUnixPubTime(): Long {
        if (pubDate.isBlank()) {
            return 0
        }

        return runCatching {
            ZonedDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDate)).toEpochSecond()
        }.getOrDefault(0)
    }

    fun getPostUrl(): String {
        return if (link.contains("nyaa.si", ignoreCase = true)) guid else link
    }

    fun matches(other: RssFeedItem): Boolean {
        val isU2 = link.contains("u2.dmhy.org", ignoreCase = true)
        val sameLink = link.trim().equals(other.link.trim(), ignoreCase = true)
        val sameTitle = title.trim().equals(other.title.trim(), ignoreCase = true)

        return if (isU2) sameLink || sameTitle else sameLink
    }

    companion object {
        fun from(item: Item): RssFeedItem {
            return RssFeedItem(
                title = item.title.orElse(""),
                author = item.author.orElse(""),
                category = item.categories.firstOrNull() ?: "",
                description = item.description.orElse(""),
                guid = item.guid.orElse(""),
                link = item.link.orElse(""),
                pubDate = item.pubDate.orElse("")
            )
        }
    }
}
