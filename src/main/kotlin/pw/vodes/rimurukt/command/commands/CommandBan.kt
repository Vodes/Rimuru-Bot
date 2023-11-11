package pw.vodes.rimurukt.command.commands

import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import org.javacord.api.interaction.SlashCommandOption
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.audit.AuditLogs
import pw.vodes.rimurukt.audit.StaffAction
import pw.vodes.rimurukt.audit.StaffActionType
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.deleteAfter
import pw.vodes.rimurukt.epochSecond
import pw.vodes.rimurukt.reply
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

class CommandBan : Command("Ban", arrayOf("ban"), CommandType.MOD, "ban") {

    init {
        usage = "`${Main.config.commandPrefix}ban <user_id/mentioned users> \"reason for ban\"`"
    }

    override fun run(event: MessageCreateEvent) {
        if (args(event)[1].isEmpty())
            event.channel.sendMessage(usage).also { return }

        val users = listedUsers(event.messageContent)
        if (users.isEmpty())
            event.channel.sendMessage("No valid users were passed.").also { return }

        users.forEach {
            if (!canKickOrBan(event.messageAuthor.asUser().get(), it, event.server.get(), false)) {
                event.channel.sendMessage("Cannot ban ${it.name}.").deleteAfter(8)
            } else {
                event.server.get().banUser(it).join()
            }
        }
        event.message.deleteAfter(1, TimeUnit.SECONDS)
    }

    override fun getSlashCommandBuilder() = SlashCommandBuilder()
        .setName("ban")
        .setDescription("Bans users from the server.")
        .setEnabledInDms(false)
        .addOption(SlashCommandOption.createUserOption("user", "User to ban", true))
        .addOption(SlashCommandOption.createStringOption("reason", "Reason for banning", false))

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        val target = interaction.arguments[0].userValue.get()
        val server = interaction.server.getOrNull() ?: Main.server

        if (!canKickOrBan(interaction.user, target, server, false))
            interaction.reply("Cannot ban this user.", true).also { return }

        server.banUser(target).thenAccept {
            interaction.reply("User banned.", true)
            AuditLogs.registerStaffAction(
                StaffAction(
                    target.idAsString,
                    interaction.user.idAsString,
                    epochSecond(),
                    StaffActionType.BAN,
                    interaction.arguments.getOrNull(1)?.stringValue?.orElse("") ?: ""
                )
            )
        }.exceptionally {
            interaction.reply("Failed to ban user. Please check the logs.", true)
            null
        }
    }
}