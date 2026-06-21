package pw.vodes.rimuru.services.styx

import moe.styx.common.data.ShowVoting
import moe.styx.db.tables.ShowVotingTable
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import pw.vodes.rimuru.Main
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.services.logging.GuildExceptionLogService
import pw.vodes.rimuru.util.dbClient
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object StyxVotingService {
    private const val CLEANUP_INTERVAL_MINUTES = 10L
    private const val LEGACY_VOTE_EMOJI_ID = 778261264454385675L
    private const val VOTE_EMOJI = "✅"
    private const val STYX_ICON_URL = "https://raw.githubusercontent.com/Vodes/Styx-2/master/src/main/resources/icons/icon.png"
    private val voteEmoji = Emoji.fromUnicode(VOTE_EMOJI)
    private val voteLock = Any()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rimuru-styx-voting-cleanup").apply { isDaemon = true }
    }
    private var cleanupTask: ScheduledFuture<*>? = null

    @Volatile
    private var botOwnerId: Long? = null

    fun start() {
        if (cleanupTask != null && cleanupTask?.isCancelled == false) {
            return
        }

        cleanupTask = scheduler.scheduleAtFixedRate(
            { runCatching { cleanupMissingVotingMessages() }.onFailure { GuildExceptionLogService.report("Styx voting cleanup failed", it) } },
            30,
            CLEANUP_INTERVAL_MINUTES * 60,
            TimeUnit.SECONDS
        )
    }

    fun shutdown() {
        cleanupTask?.cancel(false)
        scheduler.shutdownNow()
    }

    fun voteEmoji(): Emoji = voteEmoji

    fun voteEmoji(guild: Guild): Emoji {
        return guild.getEmojiById(LEGACY_VOTE_EMOJI_ID)?.let { Emoji.fromCustom(it) } ?: voteEmoji
    }

    fun onReactionAdd(event: MessageReactionAddEvent) {
        if (event.user?.isBot == true || !isVotingReaction(event)) {
            return
        }
        updateVoteFromReaction(event)
    }

    fun onReactionRemove(event: MessageReactionRemoveEvent) {
        if (!isVotingReaction(event)) {
            return
        }
        updateVoteFromReaction(event)
    }

    fun createInitialVoting(title: String, anilistId: Int, message: Message): ShowVoting {
        val voting = ShowVoting(
            title = title,
            anilistID = anilistId,
            votes = 0,
            hasVeto = false,
            serverID = message.guild.idLong,
            channelID = message.channel.idLong,
            messageID = message.idLong
        )
        dbClient.transaction {
            ShowVotingTable.upsertItem(voting)
        }
        return voting
    }

    fun setVotingAuthor(embed: EmbedBuilder, completed: Boolean = false): EmbedBuilder {
        val config = ConfigService.getAppConfigBlocking().styxConfig
        return embed.setAuthor("Styx Voting${if (completed) " ($VOTE_EMOJI)" else ""}", config.styxInstanceURL.ifBlank { null }, STYX_ICON_URL)
    }

    private fun isVotingReaction(event: GenericMessageReactionEvent): Boolean {
        if (!event.isFromGuild || !ConfigService.isStyxEnabledForGuild(event.guild.idLong)) {
            return false
        }
        return when (event.emoji.type) {
            Emoji.Type.UNICODE -> event.emoji.name == VOTE_EMOJI
            Emoji.Type.CUSTOM -> event.emoji.asCustom().idLong == LEGACY_VOTE_EMOJI_ID
        }
    }

    private fun updateVoteFromReaction(event: GenericMessageReactionEvent) {
        thread(start = true, isDaemon = true, name = "styx-voting-${event.messageId}") {
            runCatching {
                synchronized(voteLock) {
                    val voting = findVoting(event.messageIdLong) ?: return@synchronized
                    if (voting.serverID != event.guild.idLong || voting.channelID != event.channel.idLong) {
                        return@synchronized
                    }

                    val channel = event.guild.getChannelById(GuildMessageChannel::class.java, voting.channelID)
                        ?: return@synchronized
                    val message = channel.retrieveMessageById(voting.messageID).complete()
                    val users = voteUsers(message)
                    val hasVeto = users.any { isVetoUser(event.guild, it.idLong) }
                    val updated = voting.copy(votes = users.size, hasVeto = voting.hasVeto || hasVeto)
                    dbClient.transaction {
                        ShowVotingTable.upsertItem(updated)
                    }

                    if (shouldLogCompleted(voting, updated)) {
                        markMessageCompleted(message)
                        logCompletedVoting(event.guild, message, updated, users)
                    }
                }
            }.onFailure {
                GuildExceptionLogService.report(event.guild.idLong, "StyxVotingService", it)
            }
        }
    }

    private fun shouldLogCompleted(previous: ShowVoting, current: ShowVoting): Boolean {
        val wasComplete = previous.votes >= 2 || previous.hasVeto
        val isComplete = current.votes >= 2 || current.hasVeto
        return !wasComplete && isComplete
    }

    private fun findVoting(messageId: Long): ShowVoting? {
        return dbClient.transaction {
            ShowVotingTable.query {
                selectAll().where { ShowVotingTable.messageID eq messageId }.toList()
            }.firstOrNull()
        }
    }

    private fun cleanupMissingVotingMessages() {
        val votings = dbClient.transaction {
            ShowVotingTable.query { selectAll().toList() }
        }

        votings.forEach { voting ->
            runCatching {
                if (votingMessageMissing(voting)) {
                    deleteVoting(voting)
                }
            }.onFailure {
                GuildExceptionLogService.report(voting.serverID, "Styx voting cleanup: failed to check ${voting.anilistID}", it)
            }
        }
    }

    private fun votingMessageMissing(voting: ShowVoting): Boolean {
        val guild = Main.jda.getGuildById(voting.serverID) ?: return false
        val channel = guild.getChannelById(GuildMessageChannel::class.java, voting.channelID) ?: return true

        return try {
            channel.retrieveMessageById(voting.messageID).complete()
            false
        } catch (e: ErrorResponseException) {
            when (e.errorResponse) {
                ErrorResponse.UNKNOWN_MESSAGE,
                ErrorResponse.UNKNOWN_CHANNEL -> true

                else -> throw e
            }
        }
    }

    private fun deleteVoting(voting: ShowVoting) {
        synchronized(voteLock) {
            dbClient.transaction {
                ShowVotingTable.deleteWhere { anilistID eq voting.anilistID }
            }
        }
    }

    private fun voteUsers(message: Message): List<User> {
        val reaction = message.reactions.firstOrNull { reaction ->
            when (reaction.emoji.type) {
                Emoji.Type.UNICODE -> reaction.emoji.name == VOTE_EMOJI
                Emoji.Type.CUSTOM -> reaction.emoji.asCustom().idLong == LEGACY_VOTE_EMOJI_ID
            }
        } ?: return emptyList()
        return reaction.retrieveUsers(MessageReaction.ReactionType.NORMAL)
            .complete()
            .filterNot { it.isBot }
            .distinctBy { it.idLong }
    }

    private fun isVetoUser(guild: Guild, userId: Long): Boolean {
        if (userId == guild.ownerIdLong) {
            return true
        }
        return userId == getBotOwnerId(guild)
    }

    private fun getBotOwnerId(guild: Guild): Long? {
        botOwnerId?.let { return it }
        return runCatching {
            guild.jda.retrieveApplicationInfo().complete().owner.idLong
        }.getOrNull()?.also { botOwnerId = it }
    }

    private fun markMessageCompleted(message: Message) {
        val embed = message.embeds.firstOrNull() ?: return
        val updated = EmbedBuilder(embed)
        setVotingAuthor(updated, completed = true)
        message.editMessageEmbeds(updated.build()).queue({}, {})
    }

    private fun logCompletedVoting(guild: Guild, message: Message, voting: ShowVoting, users: List<User>) {
        val channelId = ConfigService.getGuildConfigBlocking(guild.idLong).otherLogChannelId
        val channel = channelId?.let { guild.getChannelById(GuildMessageChannel::class.java, it) } ?: return

        val userLines = users.joinToString("\n") { user ->
            val marker = if (isVetoUser(guild, user.idLong)) " (veto)" else ""
            "${user.asMention}$marker"
        }
        val embed = EmbedBuilder()
            .setTitle("Voting done")
            .setDescription(
                """
                ${voting.title}

                Users that voted:
                ${userLines.ifBlank { "none" }}
                """.trimIndent()
            )
            .addField("Votes", voting.votes.toString(), true)
            .addField("Veto", if (voting.hasVeto) "yes" else "no", true)
            .addField("Links", "[Voting](${message.jumpUrl}) [AniList](https://anilist.co/anime/${voting.anilistID})", false)
            .setTimestamp(Instant.now())
        channel.sendMessageEmbeds(embed.build()).queue({}, {})
    }
}
