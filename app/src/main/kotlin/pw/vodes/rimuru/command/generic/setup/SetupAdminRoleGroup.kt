package pw.vodes.rimuru.command.generic.setup

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import pw.vodes.rimuru.config.ConfigService

object SetupAdminRoleGroup : SetupCommandGroupHandler {
    override fun handle(event: SlashCommandInteractionEvent, guild: Guild, subcommand: String) {
        when (subcommand) {
            "add" -> add(event, guild.idLong)
            "remove" -> remove(event, guild.idLong)
            "list" -> list(event, guild, guild.idLong)
            else -> event.reply("Unknown adminrole subcommand.").setEphemeral(true).queue()
        }
    }

    private fun add(event: SlashCommandInteractionEvent, guildId: Long) {
        val role = event.getOption("role")?.asRole ?: run {
            event.reply("Missing role option.").setEphemeral(true).queue()
            return
        }
        if (role.isPublicRole) {
            event.reply("You can't add @everyone as a setup admin role.").setEphemeral(true).queue()
            return
        }

        ConfigService.updateGuildConfigBlocking(guildId) { config ->
            config.copy(adminRoleIds = config.adminRoleIds + role.idLong)
        }
        event.reply("Added ${role.asMention} as a setup admin role.").setEphemeral(true).queue()
    }

    private fun remove(event: SlashCommandInteractionEvent, guildId: Long) {
        val role = event.getOption("role")?.asRole ?: run {
            event.reply("Missing role option.").setEphemeral(true).queue()
            return
        }
        val config = ConfigService.getGuildConfigBlocking(guildId)
        if (!config.adminRoleIds.contains(role.idLong)) {
            event.reply("${role.asMention} is not configured as a setup admin role.")
                .setEphemeral(true)
                .queue()
            return
        }

        ConfigService.updateGuildConfigBlocking(guildId) { current ->
            current.copy(adminRoleIds = current.adminRoleIds - role.idLong)
        }
        event.reply("Removed ${role.asMention} from setup admin roles.").setEphemeral(true).queue()
    }

    private fun list(event: SlashCommandInteractionEvent, guild: Guild, guildId: Long) {
        val config = ConfigService.getGuildConfigBlocking(guildId)
        if (config.adminRoleIds.isEmpty()) {
            event.reply("No setup admin roles configured. Server administrators and the server owner can still use `/setup`.")
                .setEphemeral(true)
                .queue()
            return
        }

        val lines = config.adminRoleIds.sorted().map { roleId ->
            guild.getRoleById(roleId)?.asMention ?: "`$roleId` (missing)"
        }
        event.reply(lines.joinToString(separator = "\n", prefix = "Setup admin roles:\n"))
            .setEphemeral(true)
            .queue()
    }
}
