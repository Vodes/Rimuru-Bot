package pw.vodes.rimurukt.command.commands

import org.javacord.api.entity.user.User
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.components.MultiPageEmbed

class CommandHelp : Command("Help", arrayOf("help", "h"), slashCommandName = "help") {

    private fun getEmbed(user: User): MultiPageEmbed {
        val commit = Main.config.updateConfig.currentCommit
        val version = if (commit.isBlank()) Main.VERSION else
            "${Main.VERSION} [@${commit.subSequence(0, 6)}](https://github.com/Vodes/Rimuru-Bot/commit/$commit)"

        val commands = Commands.commands
            .filter { !it.name.equals("help", true) }
            .sortedBy { it.name }

        return MultiPageEmbed(user) {
            it.setTitle("Rimuru-Bot")
                .setAuthor(user)
                .setThumbnail(Main.api.yourself.getAvatar(4096))
                .addField("Version", version, true)
                .addField("Written by", "<@129871096299126784>", true)
                .addField("Source code", "https://github.com/Vodes/Rimuru-Bot/tree/rewrite-kotlin", false)
        }.addPage {
            it.setTitle("Commands").setAuthor(user)
            commands.forEachIndexed { index, command ->
                it.addField("${command.name}${if (command.enabled) "" else " (disabled)"}", command.type.toString(), index % 2 == 0)
            }
            it
        }
    }

    override fun run(event: MessageCreateEvent) {
        getEmbed(event.messageAuthor.asUser().get()).sendMessage(event)
    }

    override fun getSlashCommandBuilder(): SlashCommandBuilder? {
        return SlashCommandBuilder().setEnabledInDms(false).setName("help").setDescription("Displays available commands and general information on the bot.")
    }

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        getEmbed(interaction.user).sendAsResponse(interaction)
    }
}