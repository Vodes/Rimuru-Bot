package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import pw.vodes.rimuru.services.rss.RssFeed
import pw.vodes.rimuru.services.rss.RssFeedService

class CommandRss : Command("rss", CommandType.ADMIN, "Manage RSS feeds") {
    override fun guildOnly() = true

    override fun createCommand() = slashCommand()
        .addSubcommands(
            SubcommandData("add", "Add a new RSS feed")
                .addOption(OptionType.STRING, "name", "Display name for the feed", true)
                .addOption(OptionType.STRING, "url", "RSS feed URL", true)
                .addOptions(channelOption(required = true))
                .addOption(OptionType.STRING, "regex", "Only post matching entries", false),
            SubcommandData("edit", "Edit an existing RSS feed")
                .addOption(OptionType.STRING, "feed", "Feed to edit", true, true)
                .addOption(OptionType.STRING, "name", "New display name", false)
                .addOption(OptionType.STRING, "url", "New RSS feed URL", false)
                .addOptions(channelOption(required = false))
                .addOption(OptionType.STRING, "regex", "New regex filter", false),
            SubcommandData("remove", "Remove an RSS feed"),
            SubcommandData("list", "List configured RSS feeds")
        )

    override fun run(event: SlashCommandInteractionEvent) {
        val guildContext = requireGuildContext(event, requireConfiguredAdmin = true) ?: return
        when (event.subcommandName) {
            "add" -> add(event, guildContext.guild)
            "edit" -> edit(event, guildContext.guild)
            "remove" -> openRemoveSelector(event, guildContext.guild)
            "list" -> list(event, guildContext.guild)
            else -> event.reply("Unknown RSS subcommand.").setEphemeral(true).queue()
        }
    }

    override fun onAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        if (event.subcommandName != "edit" || event.focusedOption.name != "feed") {
            return
        }

        val guild = event.guild ?: run {
            event.replyChoices(emptyList()).queue()
            return
        }
        val query = event.focusedOption.value.lowercase()
        val choices = guildFeedEntries(guild)
            .filter { entry ->
                query.isBlank()
                    || entry.value.name.lowercase().contains(query)
                    || entry.value.url.lowercase().contains(query)
            }
            .take(MAX_AUTOCOMPLETE_CHOICES)
            .map { entry ->
                Choice(feedChoiceLabel(entry.index, entry.value), entry.index.toString())
            }

        event.replyChoices(choices).queue()
    }

    companion object {
        private const val REMOVE_MENU_PREFIX = "rss:remove"
        private const val MAX_REMOVE_OPTIONS = 25
        private const val MAX_AUTOCOMPLETE_CHOICES = 25

        fun onSelectInteraction(event: StringSelectInteractionEvent) {
            val context = parseRemoveMenuContext(event.componentId) ?: return
            if (event.values.isEmpty()) {
                event.reply("Missing RSS feed selection.").setEphemeral(true).queue()
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

            val guild = event.guild ?: run {
                event.reply("This action can only be used in a server.").setEphemeral(true).queue()
                return
            }
            val index = event.values.first().toIntOrNull() ?: run {
                event.reply("Invalid RSS feed selection.").setEphemeral(true).queue()
                return
            }

            val feeds = RssFeedService.getFeeds()
            val feed = feeds.getOrNull(index)
            if (feed == null || feed.guildId != guild.idLong) {
                event.editMessage("That RSS feed no longer exists.").setComponents().queue()
                return
            }

            RssFeedService.replaceFeeds(feeds.filterIndexed { currentIndex, _ -> currentIndex != index })
            event.editMessage("RSS feed removed: ${feedSummary(feed)}").setComponents().queue()
        }

        private fun channelOption(required: Boolean): OptionData {
            return OptionData(OptionType.CHANNEL, "channel", "Channel to post new entries in", required)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
        }

        private fun guildFeedEntries(guild: Guild): List<IndexedValue<RssFeed>> {
            return RssFeedService.getFeeds()
                .withIndex()
                .filter { it.value.guildId == guild.idLong }
        }

        private fun feedChoiceLabel(index: Int, feed: RssFeed): String {
            val base = "${index + 1}. ${feed.name}"
            return if (base.length <= 100) base else base.take(97) + "..."
        }

        private fun feedSummary(feed: RssFeed): String {
            val regexSummary = feed.regex.ifBlank { "none" }
            return "`${feed.name}` -> <#${feed.channelId}> | ${feed.url} | regex: `$regexSummary`"
        }

        private data class RemoveMenuContext(
            val guildId: Long,
            val userId: Long
        )

        private fun parseRemoveMenuContext(componentId: String): RemoveMenuContext? {
            val parts = componentId.split(':')
            if (parts.size != 4) {
                return null
            }
            if (parts[0] != "rss" || parts[1] != "remove") {
                return null
            }
            val guildId = parts[2].toLongOrNull() ?: return null
            val userId = parts[3].toLongOrNull() ?: return null
            return RemoveMenuContext(guildId, userId)
        }
    }

    private fun add(event: SlashCommandInteractionEvent, guild: Guild) {
        val name = event.getOption("name")?.asString?.trim().orEmpty()
        val url = event.getOption("url")?.asString?.trim().orEmpty()
        val channel = resolveChannel(event, guild, required = true) ?: return
        val regex = event.getOption("regex")?.asString?.trim().orEmpty()

        if (name.isBlank() || url.isBlank()) {
            event.reply("Missing required RSS feed fields.").setEphemeral(true).queue()
            return
        }

        val entry = RssFeed(
            name = name,
            url = url,
            regex = regex,
            guildId = guild.idLong,
            channelId = channel.idLong
        )
        RssFeedService.replaceFeeds(RssFeedService.getFeeds() + entry)

        event.reply("RSS feed added: ${feedSummary(entry)}").setEphemeral(true).queue()
    }

    private fun edit(event: SlashCommandInteractionEvent, guild: Guild) {
        val index = event.getOption("feed")?.asString?.toIntOrNull() ?: run {
            event.reply("Invalid RSS feed selection.").setEphemeral(true).queue()
            return
        }

        val currentFeeds = RssFeedService.getFeeds()
        val existing = currentFeeds.getOrNull(index)
        if (existing == null || existing.guildId != guild.idLong) {
            event.reply("That RSS feed no longer exists in this server.").setEphemeral(true).queue()
            return
        }

        val updatedChannel = if (event.getOption("channel") != null) {
            resolveChannel(event, guild, required = false) ?: return
        } else {
            null
        }

        val updated = existing.copy(
            name = event.getOption("name")?.asString?.trim().orEmpty().ifBlank { existing.name },
            url = event.getOption("url")?.asString?.trim().orEmpty().ifBlank { existing.url },
            regex = event.getOption("regex")?.asString?.trim() ?: existing.regex,
            channelId = updatedChannel?.idLong ?: existing.channelId
        )

        val feeds = currentFeeds.toMutableList()
        feeds[index] = updated
        RssFeedService.replaceFeeds(feeds)

        event.reply("RSS feed updated: ${feedSummary(updated)}").setEphemeral(true).queue()
    }

    private fun openRemoveSelector(event: SlashCommandInteractionEvent, guild: Guild) {
        val entries = guildFeedEntries(guild)
        if (entries.isEmpty()) {
            event.reply("No RSS feeds configured in this server.").setEphemeral(true).queue()
            return
        }

        val removeMenuId = "$REMOVE_MENU_PREFIX:${guild.idLong}:${event.user.idLong}"
        val menu = StringSelectMenu.create(removeMenuId)
            .setPlaceholder("Select an RSS feed to remove")
            .setRequiredRange(1, 1)

        entries.take(MAX_REMOVE_OPTIONS).forEach { entry ->
            val description = entry.value.url.take(100)
            menu.addOption(feedChoiceLabel(entry.index, entry.value), entry.index.toString(), description)
        }

        val suffix = if (entries.size > MAX_REMOVE_OPTIONS) "\nOnly the first $MAX_REMOVE_OPTIONS feeds are shown." else ""
        event.reply("Select an RSS feed to remove.$suffix")
            .addComponents(ActionRow.of(menu.build()))
            .setEphemeral(true)
            .queue()
    }

    private fun list(event: SlashCommandInteractionEvent, guild: Guild) {
        val entries = guildFeedEntries(guild)
        if (entries.isEmpty()) {
            event.reply("No RSS feeds configured in this server.").setEphemeral(true).queue()
            return
        }

        val lines = entries.map { entry ->
            "${entry.index + 1}. ${feedSummary(entry.value)}"
        }
        event.reply(lines.joinToString(separator = "\n", prefix = "RSS feeds:\n"))
            .setEphemeral(true)
            .queue()
    }

    private fun resolveChannel(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        required: Boolean
    ): GuildMessageChannel? {
        val option = event.getOption("channel")
        if (option == null) {
            if (required) {
                event.reply("Missing channel option.").setEphemeral(true).queue()
            }
            return null
        }

        val channel = option.asChannel.asGuildMessageChannel()
        if (channel.guild.idLong != guild.idLong) {
            event.reply("Channel must belong to this server and support messages.").setEphemeral(true).queue()
            return null
        }
        if (!channel.canTalk()) {
            event.reply("I can't send messages in ${channel.asMention}.").setEphemeral(true).queue()
            return null
        }

        return channel
    }
}
