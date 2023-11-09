package pw.vodes.rimurukt.command.commands

import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.updater.Updater

class CommandUpdate : Command("Update", arrayOf("update"), CommandType.ADMIN) {
    override fun run(event: MessageCreateEvent) {
        if (!Updater.buildGit(event))
            event.channel.sendMessage("Failed to build. Please check the logs.").also { return }

        if (!Updater.restart())
            event.channel.sendMessage("Failed to restart. Please check the logs.").also { return }
    }
}