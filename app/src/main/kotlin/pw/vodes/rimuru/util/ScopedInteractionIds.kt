package pw.vodes.rimuru.util

data class ScopedInteractionId(
    val guildId: Long,
    val userId: Long,
    val extraParts: List<String> = emptyList()
)

object ScopedInteractionIds {
    fun create(prefix: String, guildId: Long, userId: Long, vararg extraParts: String): String {
        return buildList {
            addAll(prefixParts(prefix))
            add(guildId.toString())
            add(userId.toString())
            addAll(extraParts)
        }.joinToString(":")
    }

    fun parse(customId: String, prefix: String): ScopedInteractionId? {
        val customIdParts = customId.split(':')
        val prefixParts = prefixParts(prefix)
        if (customIdParts.size < prefixParts.size + 2) {
            return null
        }
        if (customIdParts.subList(0, prefixParts.size) != prefixParts) {
            return null
        }

        val guildId = customIdParts[prefixParts.size].toLongOrNull() ?: return null
        val userId = customIdParts[prefixParts.size + 1].toLongOrNull() ?: return null
        return ScopedInteractionId(guildId, userId, customIdParts.drop(prefixParts.size + 2))
    }

    private fun prefixParts(prefix: String): List<String> {
        return prefix.split(':').filter { it.isNotBlank() }
    }
}
