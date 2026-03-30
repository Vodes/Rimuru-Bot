package pw.vodes.rimuru.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val styxEnabledGuildId: Long? = null,
    val styxConfig: StyxConfig = StyxConfig()
)
