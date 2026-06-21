package pw.vodes.rimuru.config

import kotlinx.serialization.Serializable

@Serializable
data class AutoRoleConfig(
    val channelId: Long,
    val messageId: Long,
    val roleId: Long
)
