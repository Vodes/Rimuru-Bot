package pw.vodes.rimurukt.command.commands

import org.javacord.api.entity.channel.ChannelType
import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.server.Server
import org.javacord.api.interaction.*
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.eqI
import pw.vodes.rimurukt.lightEscapeURL
import pw.vodes.rimurukt.reply
import pw.vodes.rimurukt.services.rss.Feed
import pw.vodes.rimurukt.services.rss.RSSFeeds
import kotlin.jvm.optionals.getOrNull

class CommandRSS : Command("rss", type = CommandType.MOD, slashCommandName = "rss") {

    override fun getSlashCommandBuilder(): SlashCommandBuilder? {
        val builder = SlashCommandBuilder()
            .setEnabledInDms(false)
            .setName(slashCommandName)
            .setDescription("Manage RSS Feeds")

        val addArgs = listOf(
            SlashCommandOption.createStringOption("Name", "Name shown in lists and whatnot", true),
            SlashCommandOption.createStringOption("URL", "URL for the RSS Feed", true),
            SlashCommandOption.createChannelOption(
                "Channel",
                "Channel in which the new entries will be posted",
                true,
                setOf(ChannelType.SERVER_TEXT_CHANNEL)
            ),
            SlashCommandOption.createStringOption("Regex", "Only posts matching this regex will be posted if given", false)
        )

        val editArgs = listOf(
            SlashCommandOption.createStringOption("Name", "Name shown in lists and whatnot", false),
            SlashCommandOption.createStringOption("URL", "URL for the RSS Feed", false),
            SlashCommandOption.createChannelOption(
                "Channel",
                "Channel in which the new entries will be posted",
                false,
                setOf(ChannelType.SERVER_TEXT_CHANNEL)
            ),
            SlashCommandOption.createStringOption("Regex", "Only posts matching this regex will be posted if given", false)
        )

        val choice = SlashCommandOption.createWithChoices(
            SlashCommandOptionType.LONG, "Feed", "Feed to edit/remove", true,
            // TODO: Figure out why this doesn't get updated?, Perhaps the longs should be unique? or maybe use something else
            RSSFeeds.feeds.mapIndexed { index, feed ->
                SlashCommandOptionChoice.create(feed.name, index.toLong()).also { println("Registering ${index.toLong()} to ${feed.name}") }
            },
        )

        builder.addOption(SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "add", "Add new RSS Feed", addArgs))
        builder.addOption(
            SlashCommandOption.createWithOptions(
                SlashCommandOptionType.SUB_COMMAND,
                "edit",
                "Edit RSS Feed",
                mutableListOf(choice).apply { this.addAll(editArgs) })
        )

        if (RSSFeeds.feeds.isNotEmpty())
            builder.addOption(
                SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "remove", "Remove RSS Feed", listOf(choice))
            )

        builder.addOption(SlashCommandOption.createSubcommand("list", "List current rss feeds"))

        return builder
    }

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        val opt = interaction.options.getOrNull(0)
        if (opt == null) {
            interaction.reply("What, how?", true)
            return
        }
        when (opt.name) {
            "add", "edit" -> {
                val channel = opt.getOptionByName("Channel").getOrNull()?.channelValue?.getOrNull()?.asServerTextChannel()?.getOrNull()
                val regex = opt.getOptionByName("Regex").getOrNull()?.stringValue?.getOrNull()
                if (opt.name eqI "add") {
                    if (channel == null) {
                        interaction.reply("No valid channel was provided.")
                        return
                    }
                }
                if (channel != null && (!channel.canYouSee() || !channel.canYouWrite()))
                    interaction.reply("The bot does not have sufficient permissions for this channel!").also { return }

                if (opt.name eqI "edit") {
                    val name = opt.getOptionByName("Name").getOrNull()?.stringValue?.getOrNull() ?: ""
                    val url = opt.getOptionByName("URL").getOrNull()?.stringValue?.getOrNull() ?: ""
                    val index = opt.getOptionByName("Feed").get().longValue.get().toInt()
                    println("Edit command received $index as index?")
                    val existing = RSSFeeds.feeds[index]
                    val feed = existing.copy(
                        name = name.ifBlank { existing.name },
                        url = url.ifBlank { existing.url },
                        regex = regex ?: existing.regex,
                        serverID = channel?.server?.idAsString ?: existing.serverID,
                        channelID = channel?.idAsString ?: existing.channelID
                    )
                    RSSFeeds.feeds[index] = feed
                    interaction.reply("RSS Feed edited.")
                } else {
                    if (channel == null) {
                        interaction.reply("No valid channel was provided.")
                        return
                    }
                    val name = opt.getOptionByName("Name").get().stringValue.get()
                    val url = opt.getOptionByName("URL").get().stringValue.get()
                    RSSFeeds.feeds.add(Feed(name, url, regex ?: "", channel.server.idAsString, channel.idAsString))
                    interaction.reply("RSS Feed added.")
                }

                RSSFeeds.save()
                Commands.updateSlashCommands()
            }

            "remove" -> {
                RSSFeeds.feeds.removeAt(opt.options[0].longValue.get().toInt())
                RSSFeeds.save()
                Commands.updateSlashCommands()
                interaction.reply("RSS Feed removed.")
            }

            "list" -> interaction.createImmediateResponder().addEmbed(listEmbed(interaction.server.get())).setFlags(MessageFlag.EPHEMERAL).respond()

            else -> interaction.createImmediateResponder().setContent("Somehow passed a non valid action?").setFlags(MessageFlag.EPHEMERAL).respond()
        }
    }

    private fun listEmbed(server: Server): EmbedBuilder {
        val embed = EmbedBuilder().setTitle("RSS Feeds")

        val feeds = RSSFeeds.feeds.filter { it.serverID eqI server.idAsString }

        if (feeds.isEmpty())
            embed.setDescription("There are currently no feeds on this server.")
        else
            feeds.forEachIndexed { index, feed ->
                var shortURL = feed.url.lightEscapeURL().removePrefix("https://").removePrefix("http://")
                shortURL = if (shortURL.length > 170) "${shortURL.substring(0, 169)}..." else shortURL
                embed.addField("${index + 1}. ${feed.name}", "in: ${feed.channel().mentionTag}\n[$shortURL](${feed.url.lightEscapeURL()})")
            }
        return embed
    }
}