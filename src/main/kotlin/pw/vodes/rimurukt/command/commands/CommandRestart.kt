package pw.vodes.rimurukt.command.commands

import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.updater.Updater

class CommandRestart : Command("Restart", arrayOf("restart"), CommandType.MOD) {
    override fun run(event: MessageCreateEvent) {
        if (!Updater.restart())
            event.channel.sendMessage("Failed to restart. Please check the logs.").also { return }
    }
}