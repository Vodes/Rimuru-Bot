package pw.vodes.rimuru.util

object DiscordMessageLinks {
    private val MESSAGE_LINK_REGEX =
        Regex("^https?://(?:canary\\.|ptb\\.)?discord(?:app)?\\.com/channels/(@me|\\d+)/(\\d+)/(\\d+)(?:/)?$")

    fun parse(link: String): MessageLink? {
        val match = MESSAGE_LINK_REGEX.matchEntire(link) ?: return null
        val channelId = match.groupValues[2].toLongOrNull() ?: return null
        val messageId = match.groupValues[3].toLongOrNull() ?: return null
        return MessageLink(
            guildId = match.groupValues[1],
            channelId = channelId,
            messageId = messageId
        )
    }

    data class MessageLink(
        val guildId: String,
        val channelId: Long,
        val messageId: Long
    ) {
        val isDm: Boolean
            get() = guildId == "@me"

        val guildIdLong: Long?
            get() = guildId.toLongOrNull()
    }
}
