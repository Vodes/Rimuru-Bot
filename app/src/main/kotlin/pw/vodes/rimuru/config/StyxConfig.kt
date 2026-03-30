package pw.vodes.rimuru.config

import kotlinx.serialization.Serializable

@Serializable
data class StyxConfig(
    val dbHost: String = "",
    val dbUsername: String = "",
    val dbPassword: String = "",
    val botDisplayName: String = "",
    val botAvatarUrl: String = "",
    val botBio: String = ""
)
