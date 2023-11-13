package pw.vodes.rimurukt.command.commands

import org.javacord.api.entity.server.Server
import org.javacord.api.entity.user.User
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import org.javacord.api.interaction.SlashCommandOption
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.addIconAsImage
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.components.MultiPageEmbed
import pw.vodes.rimurukt.getRelativeTimestamp
import kotlin.jvm.optionals.getOrNull

class CommandUserInfo : Command("UserInfo", arrayOf("ui", "userinfo", "av", "avatar"), CommandType.EVERYONE, "userinfo") {

    init {
        usage = "${Main.config.commandPrefix}userinfo <user_id/mentioned user>"
    }

    override fun getSlashCommandBuilder() = SlashCommandBuilder()
        .setName(slashCommandName)
        .setDescription("Show full-res avatar(s) of user and creation/join date.")
        .setEnabledInDms(true)
        .addOption(SlashCommandOption.createUserOption("user", "User to view", false))

    override fun run(event: MessageCreateEvent) {
        val users = this.listedUsers(event.messageContent)

        val caller = event.messageAuthor.asUser().get()
        getEmbed(caller, users.firstOrNull() ?: caller, event.server.getOrNull()).sendMessage(event)
    }

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        val target = interaction.arguments.getOrNull(0)?.userValue?.get() ?: interaction.user
        val server = interaction.server.getOrNull()

        getEmbed(interaction.user, target, server, false).sendAsResponse(interaction)
    }

    private fun getEmbed(user: User, target: User, server: Server?, allowStream: Boolean = true): MultiPageEmbed {
        val hasServerIcon = server != null && target.getServerAvatar(server, 4096).isPresent

        val multiPageEmbed = MultiPageEmbed(user) {
            it.addIconAsImage(target.getAvatar(4096), if (hasServerIcon) false else hasServerIcon)
            it.setAuthor(target)
            it.addField("Created", target.creationTimestamp.getRelativeTimestamp(), true)
            if (server != null) {
                val joined = target.getJoinedAtTimestamp(server)
                if (joined.isPresent)
                    it.addField("Joined", joined.get().getRelativeTimestamp(), true)
            }
            it.setFooter("ID: ${target.id}")
        }
        if (server != null) {
            val serverAvatar = target.getServerAvatar(server, 4096)
            if (serverAvatar.isPresent)
                multiPageEmbed.addPage {
                    it.addIconAsImage(serverAvatar.get(), false)
                    it.setAuthor(target)
                    it.addField("Created", target.creationTimestamp.getRelativeTimestamp(), true)
                    val joined = target.getJoinedAtTimestamp(server)
                    if (joined.isPresent)
                        it.addField("Joined", joined.get().getRelativeTimestamp(), true)
                    it.setFooter("ID: ${target.id}")
                }
        }

        return multiPageEmbed
    }
}