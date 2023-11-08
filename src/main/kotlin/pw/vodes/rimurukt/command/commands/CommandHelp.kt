package pw.vodes.rimurukt.command.commands

import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType

class CommandHelp : Command("Help", arrayOf("help", "h"), CommandType.EVERYONE) {

    override fun run(event: MessageCreateEvent) {
        event.channel.sendMessage("meme")
    }
}