package pw.vodes.rimurukt.command

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.javacord.api.entity.user.User
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.command.commands.CommandHelp
import pw.vodes.rimurukt.command.commands.CommandRestart
import pw.vodes.rimurukt.command.commands.CommandUpdate

enum class CommandType {
    EVERYONE, MOD, ADMIN
}

@Serializable
abstract class Command(
    val name: String,
    @Transient val alias: Array<String> = arrayOf(),
    @Transient val type: CommandType = CommandType.EVERYONE,
    @Transient val slashCommandName: String? = null
) {
    @Transient
    var usage: String? = null

    var enabled: Boolean = true

    abstract fun run(event: MessageCreateEvent)

    open fun getSlashCommandBuilder(): SlashCommandBuilder? = null

    open fun runSlashCommand(interaction: SlashCommandInteraction) {
        interaction.createImmediateResponder().respond()
    }

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
    val commands = mutableListOf<Command>()

    fun load() {
        commands.add(CommandHelp())
        commands.add(CommandUpdate())
        commands.add(CommandRestart())

        val builders = hashSetOf<SlashCommandBuilder>()
        commands.forEach {
            val builder = it.getSlashCommandBuilder() ?: return@forEach
            builders.add(builder)
        }
        Main.api.bulkOverwriteServerApplicationCommands(Main.server, builders)
        Main.api.addSlashCommandCreateListener {
            if (it.slashCommandInteraction.applicationId != Main.api.yourself.id)
                return@addSlashCommandCreateListener
            commands.forEach { cmd ->
                if (it.slashCommandInteraction.commandName.equals(cmd.slashCommandName, true)) {
                    cmd.runSlashCommand(it.slashCommandInteraction)
                    return@addSlashCommandCreateListener
                }
            }
            it.slashCommandInteraction.createImmediateResponder().respond()
        }
    }

    fun tryRunCommand(event: MessageCreateEvent) {
        val content = event.messageContent
        commands.forEach {
            if (!it.enabled)
                return@forEach

            for (alias in it.alias) {
                if (content.startsWith("${Main.config.commandPrefix}$alias", true)
                    && hasPerms(it, event.messageAuthor.asUser().get())
                ) {
                    it.run(event)
                    return
                }
            }
        }
    }

    private fun hasPerms(cmd: Command, user: User): Boolean {
        if (Main.server.isAdmin(user) || user.id == Main.api.getOwnerId().orElse(0L))
            return true

        when (cmd.type) {
            CommandType.EVERYONE -> {
                return true
            }

            CommandType.MOD -> {
                Main.config.modRoles().forEach {
                    if (it.hasUser(user))
                        return true
                }
                return false
            }

            else -> return false
        }
    }
}