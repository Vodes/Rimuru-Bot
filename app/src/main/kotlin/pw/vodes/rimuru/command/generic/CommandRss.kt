package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.components.selections.SelectOption
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
import pw.vodes.rimuru.util.AutocompleteChoices
import pw.vodes.rimuru.util.OwnedStringSelectHandler
import pw.vodes.rimuru.util.ScopedInteractionId

class CommandRss : Command("rss", CommandType.ADMIN, "Manage RSS feeds") {
    init {
        RemoveMenuHandler
    }

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
        AutocompleteChoices.replyMatching(
            event = event,
            entries = guildFeedEntries(guild),
            maxChoices = MAX_AUTOCOMPLETE_CHOICES,
            matches = { entry, query ->
                query.isBlank()
                    || entry.value.name.lowercase().contains(query)
                    || entry.value.url.lowercase().contains(query)
            },
            toChoice = { entry -> Choice(feedChoiceLabel(entry.index, entry.value), entry.index.toString()) }
        )
    }

    companion object {
        private const val REMOVE_MENU_PREFIX = "rss:remove"
        private const val MAX_REMOVE_OPTIONS = 25
        private const val MAX_AUTOCOMPLETE_CHOICES = 25

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

        private object RemoveMenuHandler : OwnedStringSelectHandler(
            prefix = REMOVE_MENU_PREFIX,
            missingSelectionMessage = "Missing RSS feed selection."
        ) {
            fun open(event: SlashCommandInteractionEvent, guild: Guild, entries: List<IndexedValue<RssFeed>>) {
                replyWithOwnedMenu(
                    event = event,
                    guildId = guild.idLong,
                    userId = event.user.idLong,
                    prompt = "Select an RSS feed to remove.",
                    placeholder = "Select an RSS feed to remove",
                    options = entries.map { entry ->
                        SelectOption.of(feedChoiceLabel(entry.index, entry.value), entry.index.toString())
                            .withDescription(entry.value.url.take(100))
                    },
                    maxOptions = MAX_REMOVE_OPTIONS,
                    overflowSuffix = { _, shown -> "\nOnly the first $shown feeds are shown." }
                )
            }

            override fun onSelection(
                event: StringSelectInteractionEvent,
                guild: Guild,
                selectedValue: String,
                context: ScopedInteractionId
            ) {
                val index = selectedValue.toIntOrNull() ?: run {
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

        RemoveMenuHandler.open(event, guild, entries)
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
