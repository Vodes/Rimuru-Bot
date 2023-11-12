package pw.vodes.rimurukt.command

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.javacord.api.entity.server.Server
import org.javacord.api.entity.user.User
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.command.commands.*
import pw.vodes.rimurukt.json
import pw.vodes.rimurukt.reply
import java.io.File

enum class CommandType {
    EVERYONE, MOD, ADMIN
}

@Serializable
data class CommandStatus(val name: String, var enabled: Boolean)

abstract class Command(
    val name: String,
    val alias: Array<String> = arrayOf(),
    val type: CommandType = CommandType.EVERYONE,
    val slashCommandName: String? = null
) {
    var usage: String? = null

    var enabled: Boolean = true

    open fun run(event: MessageCreateEvent) {
        event.channel.sendMessage("Regular command is currently not implemented.")
    }

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

        (1..10).forEach { list.add("") }

        return list.toList()
    }

    fun listedUsers(content: String): List<User> {
        val users = mutableListOf<User>()
        for (match in "<?@?(\\d{17,20})>?".toRegex().findAll(content)) {
            Main.api.getUserById(match.groups[1]!!.value).thenAccept {
                users.add(it)
            }
        }
        return users.toList()
    }

    fun getStatus(): CommandStatus = CommandStatus(this.name, this.enabled)

    internal fun canKickOrBan(user: User, target: User, server: Server, kick: Boolean = true): Boolean {
        val isModOrAdmin = Main.config.modRoles().find { it.hasUser(target) } != null || server.isAdmin(target)
        if (isModOrAdmin && !user.isBotOwner && !server.isOwner(user))
            return false
        if (target.isYourself || target.isBotOwner)
            return false

        return if (kick) server.canKickUser(Main.api.yourself, target) else server.canBanUser(Main.api.yourself, target)
    }
}

object Commands {
    private val statusFile = File(Main.appDir, "commands.json")
    val commands = mutableListOf<Command>()

    fun load() {
        commands.add(CommandHelp())
        commands.add(CommandUpdate())
        commands.add(CommandRestart())
        commands.add(CommandKick())
        commands.add(CommandBan())
        commands.add(CommandAutomod())
        commands.add(CommandAutorole())
        commands.add(CommandUserInfo())

        updateSlashCommands()

        Main.api.addSlashCommandCreateListener {
            if (it.slashCommandInteraction.applicationId != Main.api.yourself.id)
                return@addSlashCommandCreateListener
            commands.forEach { cmd ->
                if (it.slashCommandInteraction.commandName.equals(cmd.slashCommandName, true)) {
                    if (!cmd.enabled)
                        it.slashCommandInteraction.reply("This command is currently disabled!").also { return@addSlashCommandCreateListener }
                    if (!hasPerms(cmd, it.slashCommandInteraction.user))
                        it.slashCommandInteraction.reply("You don't have permissions to run this command.").also { return@addSlashCommandCreateListener }
                    cmd.runSlashCommand(it.slashCommandInteraction)
                    return@addSlashCommandCreateListener
                }
            }
            it.slashCommandInteraction.createImmediateResponder().respond()
        }

        if (statusFile.exists()) {
            for (status in json.decodeFromString<List<CommandStatus>>(statusFile.readText())) {
                val cmd = commands.find { it.name == status.name }
                if (cmd != null)
                    cmd.enabled = status.enabled
            }
        }
    }

    fun updateSlashCommands() {
        val builders = hashSetOf<SlashCommandBuilder>()
        commands.forEach {
            val builder = it.getSlashCommandBuilder() ?: return@forEach
            builders.add(builder)
        }
        Main.api.bulkOverwriteServerApplicationCommands(Main.server, builders)
    }

    fun save() {
        statusFile.writeText(json.encodeToString(commands.map { it.getStatus() }))
    }

    fun tryRunCommand(event: MessageCreateEvent) {
        val content = event.messageContent
        if (event.messageAuthor.isBotUser)
            return
        commands.forEach {
            for (alias in it.alias.toSet().apply { this.plus(it.name) }) {
                if (content.startsWith("${Main.config.commandPrefix}$alias", true)
                    && hasPerms(it, event.messageAuthor.asUser().get())
                ) {
                    if (!it.enabled) {
                        event.channel.sendMessage("This command is currently disabled!")
                        return
                    }
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