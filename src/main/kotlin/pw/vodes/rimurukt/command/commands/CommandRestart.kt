package pw.vodes.rimurukt.command.commands

import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.reply
import pw.vodes.rimurukt.updater.Updater

class CommandRestart : Command("Restart", arrayOf("restart"), CommandType.MOD, "restart") {
    override fun run(event: MessageCreateEvent) {
        if (!Updater.restart())
            event.channel.sendMessage("Failed to restart. Please check the logs.").also { return }
    }

    override fun getSlashCommandBuilder() = SlashCommandBuilder().setName("restart").setDescription("Restarts the bot.").setEnabledInDms(false)

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        if (!Commands.hasPerms(this, interaction.user))
            interaction.reply("You don't have permissions to run this command.").also { return }

        val updater = interaction.reply("Restarting...", true).join()

        if (!Updater.restart())
            updater.setContent("Failed to restart. Please check the logs.").update()
    }
}