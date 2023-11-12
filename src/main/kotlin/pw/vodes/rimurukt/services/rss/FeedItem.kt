package pw.vodes.rimurukt.services.rss

import com.apptasticsoftware.rssreader.Item
import kotlinx.serialization.Serializable
import pw.vodes.rimurukt.eqI
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val imageURLPattern =
    "[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\-\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/=]*)(.jpg|.png|.jpeg|.gif)".toRegex(RegexOption.IGNORE_CASE)
val u2AttachImagePattern = "^attachments\\/\\d{6}\\/.*".toRegex(RegexOption.IGNORE_CASE)
val u2PasskeyURLPattern = "https?:\\/\\/u2\\.dmhy\\.org\\/torrentrss\\.php.*(passkey=[^& ]*).*".toRegex(RegexOption.IGNORE_CASE)
val nyaaParsePattern = "(<.*>)(#\\d*) \\| (.+)(?:<\\/a>) \\| (\\d*\\.\\d* (?:GiB|MiB|TiB)) \\| ([^|]*) \\| (.*)".toRegex(RegexOption.IGNORE_CASE)

@Serializable
data class FeedItem(
    val title: String,
    val author: String,
    val category: String,
    val description: String,
    val guid: String,
    val link: String,
    val pubDate: String
) {
    var wasPosted = false

    fun getImage(): String? {
        if (description.isBlank())
            return null
        val match = imageURLPattern.find(description) ?: return null
        var url = match.groups[0]!!.value
        if (u2AttachImagePattern.matches(url))
            url = "https://u2.dmhy.org/$url"
        else if (url.startsWith("//"))
            url = "https:$url"

        return url
    }

    fun getUnixPubTime(): Long {
        if (pubDate.isBlank())
            return 0

        val time = ZonedDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDate))
        return time.toEpochSecond()
    }

    fun getPostURL(): String {
        // Return GUID because the link on nyaa rss is the download url for the .torrent
        return if (link.contains("nyaa.si", true))
            this.guid
        else
            this.link
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FeedItem)
            return super.equals(other)
        val isU2 = link.contains("u2.dmhy.org", true)
        val sameLink = link.trim() eqI other.link.trim()
        val sameTitle = title.trim() eqI other.title.trim()

        return if (isU2)
            sameLink || sameTitle
        else
            sameLink
    }

    companion object {
        fun ofItem(item: Item): FeedItem {
            return FeedItem(
                item.title.orElse(""),
                item.author.orElse(""),
                item.categories.firstOrNull() ?: "",
                item.description.orElse(""),
                item.guid.orElse(""),
                item.link.orElse(""),
                item.pubDate.orElse("")
            )
        }
    }
}