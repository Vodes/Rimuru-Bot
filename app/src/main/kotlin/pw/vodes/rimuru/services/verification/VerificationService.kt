package pw.vodes.rimuru.services.verification

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import pw.vodes.rimuru.Main
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.services.logging.GuildExceptionLogService
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object VerificationService {
    private const val CHALLENGE_VISIBLE_SECONDS = 15L
    private const val CHALLENGE_TIMEOUT_SECONDS = 17L
    private const val FAILED_VERIFICATION_TIMEOUT_MINUTES = 5L

    private val random = SecureRandom()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rimuru-verification-timeout").apply { isDaemon = true }
    }

    private val activeByUser = ConcurrentHashMap<ChallengeKey, ChallengeSession>()
    private val activeByThread = ConcurrentHashMap<Long, ChallengeKey>()
    private val pending = ConcurrentHashMap.newKeySet<ChallengeKey>()

    fun onReactionAdd(event: MessageReactionAddEvent) {
        val guild = event.guild
        val user = event.user ?: return
        val member = event.member ?: return
        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        val verificationRoleId = config.verificationRoleId ?: return
        val verificationChannelId = config.verificationChannelId ?: return
        val verificationMessageId = config.verificationReactionMessageId ?: return

        if (event.channel.idLong != verificationChannelId || event.messageIdLong != verificationMessageId) {
            return
        }

        if (member.roles.any { it.idLong == verificationRoleId }) {
            return
        }

        val role = guild.getRoleById(verificationRoleId) ?: return
        if (role.isPublicRole) {
            return
        }

        val key = ChallengeKey(guild.idLong, user.idLong)
        if (activeByUser.containsKey(key)) {
            return
        }
        if (!pending.add(key)) {
            return
        }

        event.retrieveMessage().queue(
            { message -> createChallengeThread(message, member, key, verificationRoleId) },
            {
                pending.remove(key)
                reportFailure(guild.idLong, "Verification: failed to retrieve verification message")(it)
            }
        )
    }

    fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild || !event.isFromThread) {
            return
        }

        val key = activeByThread[event.channel.idLong] ?: return
        val session = activeByUser[key] ?: run {
            activeByThread.remove(event.channel.idLong)
            return
        }

        if (event.author.idLong != session.userId) {
            return
        }

        val answer = event.message.contentRaw.trim().toDoubleOrNull()
        if (answer == null) {
            timeoutFailedVerification(event.member)
            failChallenge(event.guild.getThreadChannelById(session.threadId), key, "That was not a number.")
            return
        }

        val answerCorrect = if (session.hardmode) {
            answer.roundToInt() == session.answer.toInt()
        } else {
            answer == session.answer.toDouble()
        }
        if (!answerCorrect) {
            timeoutFailedVerification(event.member)
            sendHallOfShameEmbed(event, session, answer)
            failChallenge(event.guild.getThreadChannelById(session.threadId), key, "Wrong answer.")
            return
        }

        completeChallenge(event, session, key)
    }

    fun shutdown() {
        activeByUser.values.forEach { it.timeout.cancel(false) }
        activeByUser.clear()
        activeByThread.clear()
        pending.clear()
        scheduler.shutdownNow()
    }

    private fun createChallengeThread(message: Message, member: Member, key: ChallengeKey, roleId: Long) {
        val hardmode = member.user.timeCreated.isAfter(OffsetDateTime.now().minusDays(7))
        val challenge = generateChallenge(hardmode)
        message.createThreadChannel("verify-${member.effectiveName.take(70)}").queue(
            { thread ->
                pending.remove(key)
                val timeout = scheduler.schedule(
                    { timeoutChallenge(key) },
                    CHALLENGE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                )
                val session = ChallengeSession(
                    guildId = key.guildId,
                    userId = key.userId,
                    threadId = thread.idLong,
                    roleId = roleId,
                    hardmode = hardmode,
                    question = challenge.question,
                    answer = challenge.answer,
                    timeout = timeout
                )
                activeByUser[key] = session
                activeByThread[thread.idLong] = key

                thread.sendMessage("${member.asMention} ${challenge.question} You have $CHALLENGE_VISIBLE_SECONDS seconds.")
                    .queue(null, reportFailure(key.guildId, "Verification: failed to send challenge prompt"))
            },
            {
                pending.remove(key)
                reportFailure(key.guildId, "Verification: failed to create challenge thread")(it)
            }
        )
    }

    private fun timeoutChallenge(key: ChallengeKey) {
        val session = clearSession(key) ?: return
        val thread = Main.jda.getThreadChannelById(session.threadId)
        if (thread == null) {
            return
        }

        thread.sendMessage("<@${session.userId}> Time is up.")
            .silentMentions()
            .queue(
                { thread.delete().queueAfter(2, TimeUnit.SECONDS) },
                { thread.delete().queue() }
            )
    }

    private fun failChallenge(thread: ThreadChannel?, key: ChallengeKey, reason: String) {
        clearSession(key) ?: return
        if (thread == null) {
            return
        }

        thread.sendMessage(reason).queue(
            { thread.delete().queueAfter(2, TimeUnit.SECONDS) },
            { thread.delete().queue() }
        )
    }

    private fun completeChallenge(event: MessageReceivedEvent, session: ChallengeSession, key: ChallengeKey) {
        clearSession(key) ?: return

        val role = event.guild.getRoleById(session.roleId)
        val member = event.member
        if (role == null || member == null) {
            event.channel.sendMessage("Verification role could not be found. Please contact staff.").queue()
            (event.guild.getThreadChannelById(session.threadId))?.delete()?.queueAfter(2, TimeUnit.SECONDS)
            return
        }

        event.guild.addRoleToMember(member, role).reason("Passed verification challenge").queue(
            {
                event.channel.sendMessage("Verified successfully.").queue()
                (event.guild.getThreadChannelById(session.threadId))?.delete()?.queueAfter(2, TimeUnit.SECONDS)
            },
            {
                reportFailure(event.guild.idLong, "Verification: failed to assign verification role")(it)
                event.channel.sendMessage("Failed to assign verification role.").queue()
                (event.guild.getThreadChannelById(session.threadId))?.delete()?.queueAfter(2, TimeUnit.SECONDS)
            }
        )
    }

    private fun clearSession(key: ChallengeKey): ChallengeSession? {
        val session = activeByUser.remove(key) ?: return null
        activeByThread.remove(session.threadId)
        session.timeout.cancel(false)
        return session
    }

    private fun generateChallenge(hardmode: Boolean): MathChallenge {
        if (!hardmode) {
            var num1 = random.nextInt(80) + 10
            var num2 = random.nextInt(8) + 1
            var num3 = random.nextInt(20) + 1

            while (doubleVal(num1, num2, num3) != doubleVal(num1, num2, num3).roundToInt().toDouble()) {
                num1 = random.nextInt(80) + 10
                num2 = random.nextInt(8) + 1
                num3 = random.nextInt(20) + 1
            }

            return MathChallenge(
                question = "What is $num1 / $num2 - $num3",
                answer = doubleVal(num1, num2, num3).roundToInt()
            )
        }

        val num1 = random.nextDouble() * random.nextInt(50)
        val num2 = random.nextInt(20)
        val num3 = random.nextDouble() * random.nextInt(10)
        return MathChallenge(
            question = "What is $num1 / $num2 - $num3",
            answer = (num1 / num2) - num3
        )
    }

    private data class ChallengeKey(val guildId: Long, val userId: Long)

    private data class ChallengeSession(
        val guildId: Long,
        val userId: Long,
        val threadId: Long,
        val roleId: Long,
        val hardmode: Boolean,
        val question: String,
        val answer: Number,
        val timeout: ScheduledFuture<*>
    )

    private data class MathChallenge(val question: String, val answer: Number)

    private fun MessageCreateAction.silentMentions(): MessageCreateAction {
        val action = setAllowedMentions(emptySet<MentionType>())
        return runCatching { action.setSuppressedNotifications(true) }.getOrDefault(action)
    }

    private fun timeoutFailedVerification(member: Member?) {
        member ?: return
        member.timeoutFor(FAILED_VERIFICATION_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .reason("Failed verification")
            .queue(null, reportFailure(member.guild.idLong, "Verification: failed to timeout member"))
    }

    private fun formatFailedAnswer(answer: Double, hardmode: Boolean): String {
        return if (answer.roundToInt() == answer.toInt() && !hardmode) answer.roundToInt().toString() else answer.toString()
    }

    private fun doubleVal(num1: Int, num2: Int, num3: Int): Double {
        return (num1.toDouble() / num2.toDouble()) - num3.toDouble()
    }

    private fun sendHallOfShameEmbed(
        event: MessageReceivedEvent,
        session: ChallengeSession,
        answer: Double
    ) {
        val hallOfShameChannelId = ConfigService.getGuildConfigBlocking(event.guild.idLong).hallOfShameChannelId ?: return
        val hallOfShameChannel = event.guild.getTextChannelById(hallOfShameChannelId) ?: return
        val user = event.author

        val embed = EmbedBuilder()
            .setTitle("Failed verification")
            .setAuthor(user.asTag, null, user.effectiveAvatarUrl)
            .addField("Question", session.question, false)
            .addField("User's Answer", formatFailedAnswer(answer, session.hardmode), true)
            .addField("Correct Answer", session.answer.toString(), true)
            .setFooter("UserID: ${user.id}")
            .build()

        hallOfShameChannel.sendMessageEmbeds(embed)
            .queue(null, reportFailure(event.guild.idLong, "Verification: failed to post hall of shame embed"))
    }

    private fun reportFailure(guildId: Long, source: String): (Throwable) -> Unit {
        return { error -> GuildExceptionLogService.report(guildId, source, error) }
    }
}
