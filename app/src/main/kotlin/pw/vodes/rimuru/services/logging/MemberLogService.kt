package pw.vodes.rimuru.services.logging

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import pw.vodes.rimuru.Main
import pw.vodes.rimuru.config.ConfigService
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object MemberLogService {
    private const val LEAVE_DELAY_MS = 3000L
    private const val AUDIT_LOG_LOOKBACK_SECONDS = 15L

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rimuru-member-logging").apply { isDaemon = true }
    }
    private val pendingTasks = mutableListOf<ScheduledFuture<*>>()

    fun shutdown() {
        synchronized(pendingTasks) {
            pendingTasks.forEach { it.cancel(false) }
            pendingTasks.clear()
        }
        scheduler.shutdownNow()
    }

    fun onMemberJoin(guildId: Long, user: User) {
        val guild = Main.jda.getGuildById(guildId) ?: return
        val channel = resolveUserLogChannel(guild) ?: return
        channel.sendMessageEmbeds(buildJoinEmbed(user).build()).queue({}, {})
    }

    fun onMemberLeave(guildId: Long, user: User) {
        schedule(LEAVE_DELAY_MS) {
            val guild = Main.jda.getGuildById(guildId) ?: return@schedule
            val channel = resolveUserLogChannel(guild) ?: return@schedule
            val embed = buildLeaveEmbed(guild, user)
            channel.sendMessageEmbeds(embed.build()).queue({}, {})
        }
    }

    private fun schedule(delayMs: Long, action: () -> Unit) {
        lateinit var future: ScheduledFuture<*>
        future = scheduler.schedule({
            try {
                action()
            } finally {
                synchronized(pendingTasks) {
                    pendingTasks.remove(future)
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS)

        synchronized(pendingTasks) {
            pendingTasks += future
        }
    }

    private fun resolveUserLogChannel(guild: Guild): GuildMessageChannel? {
        val channelId = ConfigService.getGuildConfigBlocking(guild.idLong).userLogChannelId ?: return null
        val channel = guild.getChannelById(GuildMessageChannel::class.java, channelId) ?: return null
        return channel.takeIf {
            guild.selfMember.hasPermission(
                channel,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_EMBED_LINKS
            )
        }
    }

    private fun buildJoinEmbed(user: User): EmbedBuilder {
        val createdEpoch = user.timeCreated.toEpochSecond()
        return baseUserEmbed(user)
            .setAuthor("User joined")
            .setDescription("${user.asMention}\nCreated:\n<t:$createdEpoch:R> (<t:$createdEpoch:d> <t:$createdEpoch:t>)")
    }

    private fun buildLeaveEmbed(guild: Guild, user: User): EmbedBuilder {
        val recentAction = findRecentRemovalAction(guild, user.idLong)
        return if (recentAction == null) {
            baseUserEmbed(user)
                .setAuthor("User left")
                .setDescription(user.asMention)
        } else {
            val executor = recentAction.user
            val reasonBlock = recentAction.reason?.takeIf { it.isNotBlank() }?.let { "\nReason:\n```$it```" }.orEmpty()

            baseUserEmbed(user)
                .setTitle(
                    when (recentAction.type) {
                        ActionType.BAN -> "User banned"
                        ActionType.KICK -> "User kicked"
                        else -> "User left"
                    }
                )
                .setAuthor(executor?.asTag ?: "Unknown moderator", null, executor?.effectiveAvatarUrl)
                .setDescription(user.name + reasonBlock)
        }
    }

    private fun findRecentRemovalAction(guild: Guild, userId: Long) =
        if (!guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            null
        } else {
            runCatching {
                guild.retrieveAuditLogs()
                    .limit(25)
                    .complete()
                    .firstOrNull { entry ->
                        entry.targetIdLong == userId
                                && entry.type in setOf(ActionType.KICK, ActionType.BAN)
                                && entry.timeCreated.isAfter(
                            OffsetDateTime.now().minusSeconds(AUDIT_LOG_LOOKBACK_SECONDS)
                        )
                    }
            }.getOrNull()
        }

    private fun baseUserEmbed(user: User): EmbedBuilder {
        return EmbedBuilder()
            .setThumbnail(user.effectiveAvatar.getUrl(4096))
            .setTitle(user.name)
            .setFooter("ID: ${user.id}")
    }
}
