package pw.vodes.rimuru.services.styx

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Icon
import pw.vodes.rimuru.config.ConfigService
import java.net.URI

object StyxProfileService {
    fun applyConfiguredProfiles(guilds: Collection<Guild>) {
        guilds.forEach(::applyProfile)
    }

    fun applyProfile(guild: Guild) {
        val appConfig = ConfigService.getAppConfigBlocking()
        val styxGuildId = appConfig.styxEnabledGuildId
        val styxConfig = appConfig.styxConfig

        val nickname = if (guild.idLong == styxGuildId) {
            styxConfig.botDisplayName.trim().ifBlank { null }
        } else {
            null
        }
        val bio = if (guild.idLong == styxGuildId) {
            styxConfig.botBio.trim().ifBlank { null }
        } else {
            null
        }

        val avatar = if (guild.idLong == styxGuildId) {
            loadIcon(styxConfig.botAvatarUrl.trim())
        } else {
            null
        }

        guild.selfMember.manager
            .setNickname(nickname)
            .queue(null) { error ->
                System.err.println("Failed to update nickname for guild ${guild.id}: ${error.message}")
            }

        guild.selfMember.manager
            .setAvatar(avatar)
            .queue(null) { error ->
                System.err.println("Failed to update avatar for guild ${guild.id}: ${error.message}")
            }

        guild.selfMember.manager
            .setBio(bio)
            .queue(null) { error ->
                System.err.println("Failed to update bio for guild ${guild.id}: ${error.message}")
            }
    }

    private fun loadIcon(url: String): Icon? {
        if (url.isBlank()) {
            return null
        }

        return runCatching {
            URI(url).toURL().openStream().use(Icon::from)
        }.getOrElse { error ->
            System.err.println("Failed to load Styx avatar from '$url': ${error.message}")
            null
        }
    }
}
