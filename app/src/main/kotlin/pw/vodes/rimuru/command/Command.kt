package pw.vodes.rimuru.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import pw.vodes.rimuru.command.generic.*
import pw.vodes.rimuru.config.ConfigService

enum class CommandType {
    EVERYONE, MOD, ADMIN
}

abstract class Command(val name: String, val type: CommandType, val description: String) {

    abstract fun createCommand(): CommandData
    abstract fun run(event: SlashCommandInteractionEvent)

    open fun onAutoComplete(event: CommandAutoCompleteInteractionEvent) {}

    open fun requiredGuildPermissions(): Array<Permission> = emptyArray()

    open fun guildOnly(): Boolean = false

    open fun interactionContexts(): Array<InteractionContextType> = if (guildOnly()) {
        arrayOf(InteractionContextType.GUILD)
    } else {
        arrayOf(InteractionContextType.GUILD, InteractionContextType.BOT_DM)
    }

    protected fun slashCommand(): SlashCommandData {
        return Commands.slash(name, description).setContexts(*interactionContexts())
    }

    protected fun requireGuildContext(
        event: SlashCommandInteractionEvent,
        requireConfiguredAdmin: Boolean = false
    ): GuildContext? {
        val guild = event.guild ?: run {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue()
            return null
        }
        val member = event.member ?: run {
            event.reply("Could not resolve your member data.").setEphemeral(true).queue()
            return null
        }
        val requiredPerms = requiredGuildPermissions()
        if (requiredPerms.isNotEmpty()
            && !member.hasPermission(Permission.ADMINISTRATOR)
            && !member.hasPermission(*requiredPerms)
        ) {
            event.reply("You don't have permission to use this command.").setEphemeral(true).queue()
            return null
        }

        if (requireConfiguredAdmin && !hasConfiguredAdminAccess(guild, member)) {
            event.reply("Only configured setup admins, server administrators, or the server owner can use this.")
                .setEphemeral(true)
                .queue()
            return null
        }

        return GuildContext(guild, member)
    }

    protected fun hasConfiguredAdminAccess(guild: Guild, member: Member): Boolean {
        if (member.idLong == guild.ownerIdLong) {
            return true
        }
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return true
        }

        val adminRoleIds = ConfigService.getGuildConfigBlocking(guild.idLong).adminRoleIds
        if (adminRoleIds.isEmpty()) {
            return false
        }

        return member.roles.any { adminRoleIds.contains(it.idLong) }
    }

    data class GuildContext(val guild: Guild, val member: Member)
}

object CommandCollection {
    val commands = mutableListOf<Command>()

    init {
        commands.add(CommandPing())
        commands.add(CommandHelp())
        commands.add(CommandUserInfo())
        commands.add(CommandStealEmote())
        commands.add(CommandSetup())
        commands.add(CommandClear())
        commands.add(CommandRestart())
    }

    fun onInteraction(event: SlashCommandInteractionEvent) {
        val command = commands.find { it.name.equals(event.name, true) }
        if (command == null) {
            event.reply("Unknown command!").setEphemeral(true).queue()
            return
        }

        command.run(event)
    }

    fun onAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        commands.find { it.name.equals(event.name, true) }?.onAutoComplete(event)
    }
}
