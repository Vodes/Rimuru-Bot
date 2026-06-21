package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import pw.vodes.rimuru.util.DiscordMessageLinks
import java.net.URI

class CommandStealEmote : Command("stealemote", CommandType.MOD, "Clone a custom emoji into this server") {

    override fun requiredGuildPermissions() = arrayOf(Permission.MANAGE_GUILD_EXPRESSIONS)
    override fun guildOnly() = true

    override fun createCommand() = slashCommand()
        .addOption(OptionType.STRING, "emoji", "Emoji like <:name:id> or <a:name:id>", false)
        .addOption(OptionType.STRING, "message", "Message link to read first custom emoji from", false)
        .addOption(OptionType.STRING, "name", "New emoji name (2-32 chars, letters/numbers/_)", false)

    override fun run(event: SlashCommandInteractionEvent) {
        val guild = requireGuildContext(event)?.guild ?: return

        val emojiInput = event.getOption("emoji")?.asString?.trim().orEmpty()
        val messageLink = event.getOption("message")?.asString?.trim().orEmpty()
        val requestedName = event.getOption("name")?.asString?.trim().orEmpty()

        if (emojiInput.isBlank() && messageLink.isBlank()) {
            event.reply("Provide either `emoji` or `message`.").setEphemeral(true).queue()
            return
        }
        if (emojiInput.isNotBlank() && messageLink.isNotBlank()) {
            event.reply("Use either `emoji` or `message`, not both.").setEphemeral(true).queue()
            return
        }

        event.deferReply(true).queue()

        if (messageLink.isNotBlank()) {
            cloneFromMessageLink(event, guild, messageLink, requestedName)
            return
        }

        val match = CUSTOM_EMOJI_REGEX.matchEntire(emojiInput) ?: run {
            event.hook.editOriginal("Invalid emoji format. Use something like <:name:id> or <a:name:id>.").queue()
            return
        }

        val source = SimpleEmoji(
            animated = match.groupValues[1] == "a",
            name = match.groupValues[2],
            id = match.groupValues[3]
        )

        cloneEmoji(event, guild, source, requestedName)
    }

    private fun cloneFromMessageLink(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        messageLink: String,
        requestedName: String
    ) {
        val parsedLink = DiscordMessageLinks.parse(messageLink) ?: run {
            event.hook.editOriginal("Invalid message link.").queue()
            return
        }

        if (parsedLink.isDm) {
            event.hook.editOriginal("Message links from DMs are not supported.").queue()
            return
        }

        val channel = event.jda.getChannelById(GuildMessageChannel::class.java, parsedLink.channelId) ?: run {
            event.hook.editOriginal("Could not find the channel from that link.").queue()
            return
        }

        channel.retrieveMessageById(parsedLink.messageId).queue(
            { message ->
                val customEmoji = message.mentions.customEmojis.firstOrNull()
                if (customEmoji == null) {
                    event.hook.editOriginal("That message contains no custom emoji.").queue()
                    return@queue
                }

                cloneEmoji(event, guild, SimpleEmoji(customEmoji.isAnimated, customEmoji.name, customEmoji.id), requestedName)
            },
            {
                event.hook.editOriginal("Failed to retrieve that message.").queue()
            }
        )
    }

    private fun cloneEmoji(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        source: SimpleEmoji,
        requestedName: String
    ) {
        val name = requestedName.ifBlank { source.name }

        if (!VALID_NAME_REGEX.matches(name)) {
            event.hook.editOriginal("Emoji name must be 2-32 characters and only include letters, numbers, and underscores.")
                .queue()
            return
        }
        if (guild.emojis.any { it.name.equals(name, true) }) {
            event.hook.editOriginal("An emoji with that name already exists in this server.").queue()
            return
        }

        val extension = if (source.animated) "gif" else "png"
        val imageUrl = "https://cdn.discordapp.com/emojis/${source.id}.$extension?size=4096&quality=lossless"

        val icon = try {
            URI.create(imageUrl).toURL().openStream().use { Icon.from(it) }
        } catch (_: Exception) {
            event.hook.editOriginal("Failed to download source emoji.").queue()
            return
        }

        guild.createEmoji(name, icon).queue(
            { event.hook.editOriginal("Added emote `${it.name}`.").queue() },
            { event.hook.editOriginal("Failed to create emote: ${it.message ?: "unknown error"}").queue() }
        )
    }

    companion object {
        private val CUSTOM_EMOJI_REGEX = Regex("^<(a?):([A-Za-z0-9_]{2,32}):(\\d+)>$")
        private val VALID_NAME_REGEX = Regex("^[A-Za-z0-9_]{2,32}$")
    }

    private data class SimpleEmoji(val animated: Boolean, val name: String, val id: String)
}
