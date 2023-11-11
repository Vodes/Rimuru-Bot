package pw.vodes.rimurukt.command.commands

import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import org.javacord.api.interaction.SlashCommandOption
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.deleteAfter
import pw.vodes.rimurukt.epochSecond
import pw.vodes.rimurukt.reply
import pw.vodes.rimurukt.services.AuditLogs
import pw.vodes.rimurukt.services.StaffAction
import pw.vodes.rimurukt.services.StaffActionType
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

class CommandKick : Command("Kick", arrayOf("kick"), CommandType.MOD, "kick") {

    init {
        usage = "`${Main.config.commandPrefix}kick <user_id/mentioned users> \"reason for kick\"`"
    }

    override fun run(event: MessageCreateEvent) {
        if (args(event)[1].isEmpty())
            event.channel.sendMessage(usage).also { return }

        val users = listedUsers(event.messageContent)
        if (users.isEmpty())
            event.channel.sendMessage("No valid users were passed.").also { return }

        users.forEach {
            if (!canKickOrBan(event.messageAuthor.asUser().get(), it, event.server.get())) {
                event.channel.sendMessage("Cannot kick ${it.name}.").deleteAfter(8)
            } else {
                event.server.get().kickUser(it).join()
            }
        }
        event.message.deleteAfter(1, TimeUnit.SECONDS)
    }

    override fun getSlashCommandBuilder() = SlashCommandBuilder()
        .setName("kick")
        .setDescription("Kicks users from the server.")
        .setEnabledInDms(false)
        .addOption(SlashCommandOption.createUserOption("user", "User to kick", true))
        .addOption(SlashCommandOption.createStringOption("reason", "Reason for kicking", false))

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        val target = interaction.arguments[0].userValue.get()
        val server = interaction.server.getOrNull() ?: Main.server

        if (!canKickOrBan(interaction.user, target, server))
            interaction.reply("Cannot kick this user.", true).also { return }

        server.kickUser(target).thenAccept {
            interaction.reply("User kicked.", true)
            AuditLogs.registerStaffAction(
                StaffAction(
                    target.idAsString,
                    interaction.user.idAsString,
                    epochSecond(),
                    StaffActionType.KICK,
                    interaction.arguments.getOrNull(1)?.stringValue?.orElse("") ?: ""
                )
            )
        }.exceptionally {
            interaction.reply("Failed to kick user. Please check the logs.", true)
            null
        }
    }
}