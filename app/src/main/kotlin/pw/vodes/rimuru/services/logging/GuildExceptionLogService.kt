package pw.vodes.rimuru.services.logging

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import pw.vodes.rimuru.Main
import pw.vodes.rimuru.config.ConfigService
import java.time.Instant

object GuildExceptionLogService {
    private const val PROJECT_PACKAGE = "pw.vodes.rimuru"
    private const val MAX_MESSAGE_LENGTH = 700
    private const val MAX_STACKTRACE_LENGTH = 1200

    fun report(guildId: Long, source: String, exception: Throwable): Boolean {
        return report(source, exception, guildId)
    }

    fun report(source: String, exception: Throwable, guildId: Long? = null): Boolean {
        val guildChannel = guildId?.let(::resolveGuildExceptionLogChannel)
        if (trySend(guildChannel, source, exception)) {
            return true
        }

        val globalChannel = resolveGlobalExceptionLogChannel()
            ?.takeUnless { it.idLong == guildChannel?.idLong }
        return trySend(globalChannel, source, exception)
    }

    private fun trySend(channel: GuildMessageChannel?, source: String, exception: Throwable): Boolean {
        channel ?: return false
        return runCatching {
            channel.sendMessageEmbeds(buildEmbed(source, exception).build()).complete()
            true
        }.getOrDefault(false)
    }

    private fun resolveGuildExceptionLogChannel(guildId: Long): GuildMessageChannel? {
        val guild = Main.jda.getGuildById(guildId) ?: return null
        val channelId = ConfigService.getGuildConfigBlocking(guild.idLong).otherLogChannelId ?: return null
        val channel = guild.getChannelById(GuildMessageChannel::class.java, channelId) ?: return null
        return channel.takeIf { guild.selfMember.hasPermission(it, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS) }
    }

    private fun resolveGlobalExceptionLogChannel(): GuildMessageChannel? {
        val channelId = ConfigService.getAppConfigBlocking().globalExceptionLogChannelId ?: return null
        val channel = Main.jda.getChannelById(GuildMessageChannel::class.java, channelId) ?: return null
        return channel.takeIf { it.guild.selfMember.hasPermission(it, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS) }
    }

    private fun buildEmbed(source: String, exception: Throwable): EmbedBuilder {
        return EmbedBuilder()
            .setTitle("Exception thrown!")
            .setDescription(buildDescription(exception))
            .setTimestamp(Instant.now())
            .setFooter("Source: $source")
    }

    private fun buildDescription(exception: Throwable): String {
        val message = (exception.localizedMessage ?: exception.message ?: exception.javaClass.simpleName)
            .sanitizeForCodeBlock()
            .truncate(MAX_MESSAGE_LENGTH)
        val trace = buildStackTraceSnippet(exception)
        return if (trace.isBlank()) {
            message
        } else {
            "$message\n```$trace```"
        }
    }

    private fun buildStackTraceSnippet(exception: Throwable): String {
        val stackTrace = exception.stackTraceToString()
        if (!stackTrace.contains(PROJECT_PACKAGE)) {
            return ""
        }

        val startIndex = stackTrace.indexOf('\n').takeIf { it >= 0 }?.plus(1) ?: 0
        return stackTrace.substring(startIndex)
            .sanitizeForCodeBlock()
            .truncate(MAX_STACKTRACE_LENGTH)
    }

    private fun String.sanitizeForCodeBlock(): String {
        return replace("```", "'''").trim()
    }

    private fun String.truncate(maxLength: Int): String {
        if (length <= maxLength) {
            return this
        }
        return take(maxLength - 3).trimEnd() + "..."
    }
}
