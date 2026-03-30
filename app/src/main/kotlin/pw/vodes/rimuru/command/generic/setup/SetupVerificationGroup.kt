package pw.vodes.rimuru.command.generic.setup

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.services.verification.UnverifiedPurgeService
import pw.vodes.rimuru.util.DiscordMessageLinks

object SetupVerificationGroup : SetupCommandGroupHandler {
    override fun handle(event: SlashCommandInteractionEvent, guild: Guild, subcommand: String) {
        when (subcommand) {
            "configure" -> configure(event, guild)
            "delay" -> configureDelay(event, guild)
            "hallofshame" -> configureHallOfShame(event, guild)
            "disable" -> disable(event, guild.idLong)
            "status" -> status(event, guild)
            else -> event.reply("Unknown verification subcommand.").setEphemeral(true).queue()
        }
    }

    private fun configure(event: SlashCommandInteractionEvent, guild: Guild) {
        val role = event.getOption("role")?.asRole ?: run {
            event.reply("Missing role option.").setEphemeral(true).queue()
            return
        }
        val channelId = event.getOption("channel")?.asChannel?.idLong ?: run {
            event.reply("Missing channel option.").setEphemeral(true).queue()
            return
        }
        val channel = guild.getTextChannelById(channelId) ?: run {
            event.reply("Verification channel must be a text channel in this server.").setEphemeral(true).queue()
            return
        }
        if (role.isPublicRole) {
            event.reply("Verification role cannot be @everyone.").setEphemeral(true).queue()
            return
        }

        event.deferReply(true).queue()

        val messageLink = event.getOption("message")?.asString?.trim().orEmpty()
        val promptMessage = if (messageLink.isBlank()) {
            createVerificationPrompt(channel)
        } else {
            resolveVerificationMessageFromLink(messageLink, guild, channel)
        }

        if (promptMessage == null) {
            event.hook.editOriginal("Could not resolve that message link. It must point to a message in ${channel.asMention}.")
                .queue()
            return
        }

        promptMessage.addReaction(Emoji.fromUnicode("✅")).queue()

        ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
            config.copy(
                verificationRoleId = role.idLong,
                verificationChannelId = channel.idLong,
                verificationReactionMessageId = promptMessage.idLong
            )
        }

        event.hook.editOriginal(
            "Verification configured.\nRole: ${role.asMention}\nChannel: ${channel.asMention}\nMessage: ${promptMessage.jumpUrl}"
        ).queue()
    }

    private fun disable(event: SlashCommandInteractionEvent, guildId: Long) {
        ConfigService.updateGuildConfigBlocking(guildId) { config ->
            config.copy(
                verificationRoleId = null,
                verificationChannelId = null,
                verificationReactionMessageId = null
            )
        }
        event.reply("Verification has been disabled for this server.").setEphemeral(true).queue()
    }

    private fun configureDelay(event: SlashCommandInteractionEvent, guild: Guild) {
        val days = event.getOption("days")?.asLong?.toInt() ?: run {
            event.reply("Missing days option.").setEphemeral(true).queue()
            return
        }
        if (days !in -1..365) {
            event.reply("`days` must be between -1 and 365. Use 0 to disable auto-kick, or -1 for 30-second fast purge mode.")
                .setEphemeral(true)
                .queue()
            return
        }

        ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
            config.copy(purgeUnverifiedAfterDays = days)
        }
        if (days == -1) {
            UnverifiedPurgeService.scheduleFastPurgeForGuildMembers(guild)
        }

        val message = when (days) {
            -1 -> "Fast purge mode enabled. Unverified users are auto-kicked after 30 seconds."
            0 -> "Auto-kick for unverified users has been disabled."
            else -> "Unverified users will now be auto-kicked after $days day(s)."
        }
        event.reply(message).setEphemeral(true).queue()
    }

    private fun configureHallOfShame(event: SlashCommandInteractionEvent, guild: Guild) {
        val channelId = event.getOption("channel")?.asChannel?.idLong
        if (channelId == null) {
            ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
                config.copy(hallOfShameChannelId = null)
            }
            event.reply("Hall of shame logging disabled.").setEphemeral(true).queue()
            return
        }

        val channel = guild.getTextChannelById(channelId) ?: run {
            event.reply("Hall of shame channel must be a text channel in this server.").setEphemeral(true).queue()
            return
        }

        ConfigService.updateGuildConfigBlocking(guild.idLong) { config ->
            config.copy(hallOfShameChannelId = channel.idLong)
        }
        event.reply("Hall of shame channel set to ${channel.asMention}.").setEphemeral(true).queue()
    }

    private fun status(event: SlashCommandInteractionEvent, guild: Guild) {
        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        val roleId = config.verificationRoleId
        val channelId = config.verificationChannelId
        val messageId = config.verificationReactionMessageId
        val delay = config.purgeUnverifiedAfterDays
        val hallOfShameId = config.hallOfShameChannelId

        if (roleId == null || channelId == null || messageId == null) {
            event.reply(
                "Verification is currently not configured. Use `/setup verification configure`.\nAuto-kick delay: ${
                    purgeDelayText(delay)
                }\nHall of shame: ${hallOfShameText(guild, hallOfShameId)}"
            )
                .setEphemeral(true)
                .queue()
            return
        }

        val roleText = guild.getRoleById(roleId)?.asMention ?: "`$roleId` (missing)"
        val channelText = guild.getTextChannelById(channelId)?.asMention ?: "`$channelId` (missing)"
        val messageLink = "https://discord.com/channels/${guild.id}/$channelId/$messageId"

        event.reply(
            "Verification status:\nRole: $roleText\nChannel: $channelText\nMessage: $messageLink\nAuto-kick delay: ${
                purgeDelayText(delay)
            }\nHall of shame: ${hallOfShameText(guild, hallOfShameId)}"
        )
            .setEphemeral(true)
            .queue()
    }

    private fun createVerificationPrompt(channel: TextChannel): Message {
        val content = "React with ✅ to begin verification. You will get a math question in a thread and have 15 seconds to answer."
        return channel.sendMessage(content).complete()
    }

    private fun resolveVerificationMessageFromLink(
        messageLink: String,
        guild: Guild,
        channel: TextChannel
    ): Message? {
        val parsed = DiscordMessageLinks.parse(messageLink) ?: return null
        if (parsed.isDm || parsed.guildIdLong != guild.idLong || parsed.channelId != channel.idLong) {
            return null
        }
        return runCatching { channel.retrieveMessageById(parsed.messageId).complete() }.getOrNull()
    }

    private fun hallOfShameText(guild: Guild, channelId: Long?): String {
        if (channelId == null) {
            return "disabled"
        }
        return guild.getTextChannelById(channelId)?.asMention ?: "`$channelId` (missing)"
    }

    private fun purgeDelayText(delay: Int): String {
        return when (delay) {
            -1 -> "30 seconds (fast mode)"
            0 -> "disabled"
            else -> "$delay day(s)"
        }
    }
}
