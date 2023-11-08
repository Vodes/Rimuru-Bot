package pw.vodes.rimurukt.listeners

import kotlinx.coroutines.delay
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.user.User
import org.javacord.api.listener.server.member.ServerMemberJoinListener
import org.javacord.api.listener.server.member.ServerMemberLeaveListener
import pw.vodes.rimurukt.*
import pw.vodes.rimurukt.audit.AuditLogs

object MemberListeners {

    private fun getEmbed(user: User) = EmbedBuilder()
        .setThumbnail(user.getAvatar(4096))
        .setTitle(user.name)
        .setFooter("ID: ${user.idAsString}")

    fun JoinListener(): ServerMemberJoinListener {
        return ServerMemberJoinListener { event ->
            launchThreaded {
                delay(2500L)
                val embed = getEmbed(event.user).setAuthor("User joined")
                    .setDescription(
                        "${event.user.mentionTag}\nCreated:" +
                                "\n${event.user.creationTimestamp.getRelativeTimestamp()} (${event.user.creationTimestamp.getAbsoluteTimestamp()})"
                    )
                try {
                    Main.config.userLogChannel()!!.sendMessage(embed)
                } catch (_: Exception) {
                    LogRepeater.embedsToSend.add(embed)
                }
            }
        }
    }

    fun LeaveListener(): ServerMemberLeaveListener {
        return ServerMemberLeaveListener { event ->
            launchThreaded {
                delay(3000L)
                if (AuditLogs.wasKickedOrBanned(event.user))
                    return@launchThreaded
                val embed = getEmbed(event.user).setAuthor("User left").setDescription(event.user.mentionTag)
                try {
                    Main.config.userLogChannel()!!.sendMessage(embed)
                } catch (_: Exception) {
                    LogRepeater.embedsToSend.add(embed)
                }
            }
        }
    }
}