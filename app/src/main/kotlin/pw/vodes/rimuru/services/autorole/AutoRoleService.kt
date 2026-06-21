package pw.vodes.rimuru.services.autorole

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import pw.vodes.rimuru.config.AutoRoleConfig
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.services.logging.GuildExceptionLogService
import pw.vodes.rimuru.util.RoleSafety

object AutoRoleService {
    fun onReactionAdd(event: MessageReactionAddEvent) {
        val member = event.member ?: return
        if (member.user.isBot || member.user.isSystem) {
            return
        }
        val entries = matchingEntries(event.guild.idLong, event.channel.idLong, event.messageIdLong)
        if (entries.isEmpty()) {
            return
        }

        entries.forEach { entry ->
            val role = event.guild.getRoleById(entry.roleId) ?: return@forEach
            if (!RoleSafety.canAssignAutomatically(role)) {
                return@forEach
            }
            event.guild.addRoleToMember(member, role)
                .reason("Autorole reaction add")
                .queue(
                    null,
                    { error ->
                        GuildExceptionLogService.report(
                            event.guild.idLong,
                            "Autorole: failed to add role ${role.id}",
                            error
                        )
                    }
                )
        }
    }

    fun onReactionRemove(event: MessageReactionRemoveEvent) {
        if (event.user?.isBot == true || event.user?.isSystem == true) {
            return
        }
        val entries = matchingEntries(event.guild.idLong, event.channel.idLong, event.messageIdLong)
        if (entries.isEmpty()) {
            return
        }

        event.retrieveMember().queue { member ->
            entries.forEach { entry ->
                val role = event.guild.getRoleById(entry.roleId) ?: return@forEach
                if (role.isPublicRole || role.isManaged) {
                    return@forEach
                }
                event.guild.removeRoleFromMember(member, role)
                    .reason("Autorole reaction remove")
                    .queue(
                        null,
                        { error ->
                            GuildExceptionLogService.report(
                                event.guild.idLong,
                                "Autorole: failed to remove role ${role.id}",
                                error
                            )
                        }
                    )
            }
        }
    }

    private fun matchingEntries(guildId: Long, channelId: Long, messageId: Long): List<AutoRoleConfig> {
        return ConfigService.getGuildConfigBlocking(guildId).autoroles.filter {
            it.channelId == channelId && it.messageId == messageId
        }
    }
}
