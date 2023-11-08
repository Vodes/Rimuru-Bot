package pw.vodes.rimurukt.command

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.command.commands.CommandHelp

enum class CommandType {
    EVERYONE, MOD, ADMIN
}

@Serializable
abstract class Command(val name: String, @Transient val alias: Array<String> = arrayOf(), @Transient val type: CommandType = CommandType.EVERYONE) {
    @Transient
    var usage: String? = null

    var enabled: Boolean = true

    abstract fun run(event: MessageCreateEvent)

    fun args(event: MessageCreateEvent): List<String> {
        val list = mutableListOf<String>()

        val matches = Regex("(?=\\S)[^\\\"\\s]*(?:\\\"[^\\\\\\\"]*(?:\\\\[\\s\\S][^\\\\\\\"]*)*\\\"[^\\\"\\s]*)*").findAll(event.messageContent)

        matches.forEach {
            var s = it.groups[0]!!.value
            s = s.removePrefix("\"").removeSuffix("\"")
            list.add(s.replace("\\\"", "\""))
        }

        return list.toList()
    }
}

object Commands {
    private val commands = mutableListOf<Command>()

    fun load() {
        commands.add(CommandHelp())
    }
}