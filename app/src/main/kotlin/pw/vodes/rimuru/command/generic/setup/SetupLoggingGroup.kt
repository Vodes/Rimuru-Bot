package pw.vodes.rimuru.command.generic.setup

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import pw.vodes.rimuru.config.ConfigService

object SetupLoggingGroup : SetupCommandGroupHandler {
    override fun handle(event: SlashCommandInteractionEvent, guild: Guild, subcommand: String) {
        when (subcommand) {
            "user" -> configureUserLogging(event, guild)
            "exception" -> configureExceptionLogging(event, guild)
            "status" -> status(event, guild)
            else -> event.reply("Unknown logging subcommand.").setEphemeral(true).queue()
        }
    }

    private fun configureUserLogging(event: SlashCommandInteractionEvent, guild: Guild) {
        if (event.getOption("channel") == null) {
            ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
                config.copy(userLogChannelId = null)
            }
            event.reply("User join/leave logging disabled.").setEphemeral(true).queue()
            return
        }
        val channel = resolveLoggingChannel(event, guild, "user join/leave logging") ?: return

        ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
            config.copy(userLogChannelId = channel.idLong)
        }
        event.reply("User join/leave logging channel set to ${channel.asMention}.").setEphemeral(true).queue()
    }

    private fun configureExceptionLogging(event: SlashCommandInteractionEvent, guild: Guild) {
        if (event.getOption("channel") == null) {
            ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
                config.copy(otherLogChannelId = null)
            }
            event.reply("Exception logging disabled.").setEphemeral(true).queue()
            return
        }
        val channel = resolveLoggingChannel(event, guild, "exception logging") ?: return

        ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
            config.copy(otherLogChannelId = channel.idLong)
        }
        event.reply("Exception logging channel set to ${channel.asMention}.").setEphemeral(true).queue()
    }

    private fun status(event: SlashCommandInteractionEvent, guild: Guild) {
        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        val userLogText = channelText(guild, config.userLogChannelId)
        val exceptionLogText = channelText(guild, config.otherLogChannelId)

        event.reply(
            "Logging status:\nUser join/leave: $userLogText\nException: $exceptionLogText"
        )
            .setEphemeral(true)
            .queue()
    }

    private fun resolveLoggingChannel(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        purpose: String
    ): GuildMessageChannel? {
        val channelId = event.getOption("channel")?.asChannel?.idLong ?: return null
        return guild.getChannelById(GuildMessageChannel::class.java, channelId) ?: run {
            event.reply("The $purpose channel must be a message channel in this server.")
                .setEphemeral(true)
                .queue()
            null
        }
    }

    private fun channelText(guild: Guild, channelId: Long?): String {
        if (channelId == null) {
            return "disabled"
        }
        return guild.getGuildChannelById(channelId)?.asMention ?: "`$channelId` (missing)"
    }
}
