package pw.vodes.rimurukt.command.commands

import org.javacord.api.entity.server.Server
import org.javacord.api.entity.user.User
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
import pw.vodes.rimurukt.epochSecond
import pw.vodes.rimurukt.reply
import kotlin.jvm.optionals.getOrNull

class CommandKick : Command("Kick", arrayOf("kick"), CommandType.MOD, "kick") {

    init {
        usage = "`${Main.config.commandPrefix}kick <user_id/mentioned users> \"reason for kick\"`"
    }

    override fun run(event: MessageCreateEvent) {
        event.channel.sendMessage("Only the slash command is currently implemented.")
    }

    override fun getSlashCommandBuilder() = SlashCommandBuilder()
        .setName("kick")
        .setDescription("Kicks users from the server.")
        .setEnabledInDms(false)
        .addOption(SlashCommandOption.createUserOption("user", "User to kick", true))
        .addOption(SlashCommandOption.createStringOption("reason", "Reason for kicking", false))

    private fun canKick(user: User, target: User, server: Server): Boolean {
        val isModOrAdmin = Main.config.modRoles().find { it.hasUser(target) } != null || server.isAdmin(target)
        if (isModOrAdmin && !user.isBotOwner && !server.isOwner(user))
            return false
        if (target.isYourself || target.isBotOwner)
            return false

        return server.canKickUser(Main.api.yourself, target)
    }

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        val target = interaction.arguments[0].userValue.get()
        val server = interaction.server.getOrNull() ?: Main.server

        if (!canKick(interaction.user, target, server))
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