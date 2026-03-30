package pw.vodes.rimuru.config

import kotlinx.serialization.Serializable

@Serializable
data class GuildConfig(
    val generalChatChannelId: Long? = null,
    val userLogChannelId: Long? = null,
    val otherLogChannelId: Long? = null,
    val verificationRoleId: Long? = null,
    val verificationChannelId: Long? = null,
    val verificationReactionMessageId: Long? = null,
    val hallOfShameChannelId: Long? = null,
    val purgeUnverifiedAfterDays: Int = 3,
    val adminRoleIds: Set<Long> = emptySet(),
    val autoroles: List<AutoRoleConfig> = emptyList(),
    val automodExcludedCategoryIds: Set<Long> = emptySet()
)
