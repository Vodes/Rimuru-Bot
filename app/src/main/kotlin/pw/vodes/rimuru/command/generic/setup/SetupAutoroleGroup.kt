package pw.vodes.rimuru.command.generic.setup

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import pw.vodes.rimuru.config.AutoRoleConfig
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.util.DiscordMessageLinks

object SetupAutoroleGroup : SetupCommandGroupHandler {
    private const val REMOVE_MENU_PREFIX = "setup:autorole:remove"
    private const val MAX_REMOVE_OPTIONS = 25

    override fun handle(event: SlashCommandInteractionEvent, guild: Guild, subcommand: String) {
        when (subcommand) {
            "add" -> add(event, guild)
            "remove" -> openRemoveSelector(event, guild)
            "list" -> list(event, guild)
            else -> event.reply("Unknown autorole subcommand.").setEphemeral(true).queue()
        }
    }

    fun onSelectInteraction(event: StringSelectInteractionEvent) {
        val context = parseRemoveMenuContext(event.componentId) ?: return
        if (event.values.isEmpty()) {
            event.reply("Missing autorole selection.").setEphemeral(true).queue()
            return
        }
        if (event.guild?.idLong != context.guildId) {
            event.reply("This selector is no longer valid in this server.").setEphemeral(true).queue()
            return
        }
        if (event.user.idLong != context.userId) {
            event.reply("Only the user who opened this selector can use it.").setEphemeral(true).queue()
            return
        }

        val key = parseAutoRoleKey(event.values.first()) ?: run {
            event.reply("Invalid selection payload.").setEphemeral(true).queue()
            return
        }

        val guild = event.guild ?: run {
            event.reply("This action can only be used in a server.").setEphemeral(true).queue()
            return
        }

        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        val exists = config.autoroles.any { it.channelId == key.channelId && it.messageId == key.messageId && it.roleId == key.roleId }
        if (!exists) {
            event.editMessage("That autorole entry no longer exists.").setComponents().queue()
            return
        }

        ConfigService.updateGuildConfigBlocking(guild.idLong) { current ->
            current.copy(autoroles = current.autoroles.filterNot {
                it.channelId == key.channelId && it.messageId == key.messageId && it.roleId == key.roleId
            })
        }

        val roleText = guild.getRoleById(key.roleId)?.asMention ?: "`${key.roleId}`"
        val messageLink = "https://discord.com/channels/${guild.id}/${key.channelId}/${key.messageId}"
        event.editMessage("Autorole removed: $roleText -> $messageLink")
            .setComponents()
            .queue()
    }

    private fun add(event: SlashCommandInteractionEvent, guild: Guild) {
        val parsedInput = parseMessageAndRole(event, guild) ?: return
        val channel = guild.getChannelById(GuildMessageChannel::class.java, parsedInput.link.channelId) ?: run {
            event.reply("Could not resolve the message channel from that link.").setEphemeral(true).queue()
            return
        }

        val message = runCatching { channel.retrieveMessageById(parsedInput.link.messageId).complete() }.getOrNull() ?: run {
            event.reply("Could not retrieve that message.").setEphemeral(true).queue()
            return
        }

        val entry = AutoRoleConfig(
            channelId = parsedInput.link.channelId,
            messageId = parsedInput.link.messageId,
            roleId = parsedInput.role.idLong
        )
        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        if (config.autoroles.any { it.channelId == entry.channelId && it.messageId == entry.messageId && it.roleId == entry.roleId }) {
            event.reply("That autorole entry already exists.").setEphemeral(true).queue()
            return
        }

        ConfigService.updateGuildConfigBlocking(guild.idLong) { current ->
            current.copy(autoroles = current.autoroles + entry)
        }
        event.reply("Autorole added: `${parsedInput.role.name}` on ${message.jumpUrl}").setEphemeral(true).queue()
    }

    private fun openRemoveSelector(event: SlashCommandInteractionEvent, guild: Guild) {
        val entries = ConfigService.getGuildConfigBlocking(guild.idLong).autoroles
        if (entries.isEmpty()) {
            event.reply("No autorole entries configured.").setEphemeral(true).queue()
            return
        }

        val removeMenuId = "$REMOVE_MENU_PREFIX:${guild.idLong}:${event.user.idLong}"
        val menu = StringSelectMenu.create(removeMenuId)
            .setPlaceholder("Select an autorole to remove")
            .setRequiredRange(1, 1)

        entries.take(MAX_REMOVE_OPTIONS).forEach { entry ->
            val roleName = guild.getRoleById(entry.roleId)?.name ?: "Missing role"
            val optionValue = encodeAutoRoleKey(entry.channelId, entry.messageId, entry.roleId)
            val optionDescription = "Message ${entry.messageId} in channel ${entry.channelId}"
            menu.addOption(roleName, optionValue, optionDescription)
        }

        val suffix = if (entries.size > MAX_REMOVE_OPTIONS) "\nOnly the first $MAX_REMOVE_OPTIONS entries are shown." else ""
        event.reply("Select an autorole entry to remove.$suffix")
            .addComponents(ActionRow.of(menu.build()))
            .setEphemeral(true)
            .queue()
    }

    private fun list(event: SlashCommandInteractionEvent, guild: Guild) {
        val entries = ConfigService.getGuildConfigBlocking(guild.idLong).autoroles
        if (entries.isEmpty()) {
            event.reply("No autorole entries configured.").setEphemeral(true).queue()
            return
        }

        val lines = entries.mapIndexed { index, entry ->
            val roleText = guild.getRoleById(entry.roleId)?.asMention ?: "`${entry.roleId}` (missing)"
            val messageLink = "https://discord.com/channels/${guild.id}/${entry.channelId}/${entry.messageId}"
            "${index + 1}. $roleText -> $messageLink"
        }

        event.reply(lines.joinToString(separator = "\n", prefix = "Autoroles:\n"))
            .setEphemeral(true)
            .queue()
    }

    private fun parseMessageAndRole(event: SlashCommandInteractionEvent, guild: Guild): ParsedInput? {
        val role = event.getOption("role")?.asRole ?: run {
            event.reply("Missing role option.").setEphemeral(true).queue()
            return null
        }
        if (role.guild.idLong != guild.idLong) {
            event.reply("Role must belong to this server.").setEphemeral(true).queue()
            return null
        }

        val messageLink = event.getOption("message")?.asString?.trim().orEmpty()
        val link = DiscordMessageLinks.parse(messageLink)
        if (link == null || link.isDm || link.guildIdLong != guild.idLong) {
            event.reply("Invalid message link for this server.").setEphemeral(true).queue()
            return null
        }

        return ParsedInput(role = role, link = link)
    }

    private data class ParsedInput(
        val role: Role,
        val link: DiscordMessageLinks.MessageLink
    )

    private data class RemoveMenuContext(
        val guildId: Long,
        val userId: Long
    )

    private data class AutoRoleKey(
        val channelId: Long,
        val messageId: Long,
        val roleId: Long
    )

    private fun parseRemoveMenuContext(componentId: String): RemoveMenuContext? {
        val parts = componentId.split(':')
        if (parts.size != 5) {
            return null
        }
        if (parts[0] != "setup" || parts[1] != "autorole" || parts[2] != "remove") {
            return null
        }
        val guildId = parts[3].toLongOrNull() ?: return null
        val userId = parts[4].toLongOrNull() ?: return null
        return RemoveMenuContext(guildId, userId)
    }

    private fun encodeAutoRoleKey(channelId: Long, messageId: Long, roleId: Long): String {
        return "$channelId:$messageId:$roleId"
    }

    private fun parseAutoRoleKey(value: String): AutoRoleKey? {
        val parts = value.split(':')
        if (parts.size != 3) {
            return null
        }
        val channelId = parts[0].toLongOrNull() ?: return null
        val messageId = parts[1].toLongOrNull() ?: return null
        val roleId = parts[2].toLongOrNull() ?: return null
        return AutoRoleKey(channelId, messageId, roleId)
    }
}
