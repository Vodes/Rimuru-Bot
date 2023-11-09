package pw.vodes.rimurukt.command.commands

import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.components.MultiPageEmbed

class CommandHelp : Command("Help", arrayOf("help", "h"), CommandType.MOD) {

    override fun run(event: MessageCreateEvent) {
        val commit = Main.config.updateConfig.currentCommit
        val version = if (commit.isBlank()) Main.VERSION else
            "${Main.VERSION} [@${commit.subSequence(0, 6)}](https://github.com/Vodes/Rimuru-Bot/commit/$commit)"

        val commands = Commands.commands
            .filter { !it.name.equals("help", true) }
            .sortedBy { it.name }

        MultiPageEmbed(event.messageAuthor.asUser().get(), false) {
            it.setTitle("Rimuru-Bot")
                .setAuthor(event.messageAuthor)
                .setThumbnail(Main.api.yourself.getAvatar(4096))
                .addField("Version", version, true)
                .addField("Written by", "<@129871096299126784>", true)
                .addField("Source code", "https://github.com/Vodes/Rimuru-Bot/tree/rewrite-kotlin", false)
        }.addPage {
            it.setTitle("Commands").setAuthor(event.messageAuthor)
            commands.forEachIndexed { index, command ->
                it.addField("${command.name}${if (command.enabled) "" else " (disabled)"}", command.type.toString(), index % 2 == 0)
            }
            it
        }.sendMessage(event.channel)
    }
}