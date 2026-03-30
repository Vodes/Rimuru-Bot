package pw.vodes.rimuru.services.verification

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Guild
import pw.vodes.rimuru.Main
import pw.vodes.rimuru.config.ConfigService
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object UnverifiedPurgeService {
    private const val CHECK_INTERVAL_MINUTES = 10L
    private const val FAST_PURGE_SECONDS = 30L

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rimuru-unverified-purge").apply { isDaemon = true }
    }
    private var task: ScheduledFuture<*>? = null
    private val fastPurgeTasks = ConcurrentHashMap<MemberKey, ScheduledFuture<*>>()

    fun start() {
        if (task != null && !task!!.isCancelled) {
            return
        }

        task = scheduler.scheduleAtFixedRate(
            { runCatching { purgeUnverifiedMembers() } },
            30,
            CHECK_INTERVAL_MINUTES * 60,
            TimeUnit.SECONDS
        )
    }

    fun shutdown() {
        task?.cancel(false)
        fastPurgeTasks.values.forEach { it.cancel(false) }
        fastPurgeTasks.clear()
        scheduler.shutdownNow()
    }

    fun onMemberJoin(member: Member) {
        scheduleFastPurge(member)
    }

    fun onMemberLeave(guildId: Long, userId: Long) {
        fastPurgeTasks.remove(MemberKey(guildId, userId))?.cancel(false)
    }

    fun scheduleFastPurgeForGuildMembers(guild: Guild) {
        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        if (config.purgeUnverifiedAfterDays != -1) {
            return
        }
        guild.members.forEach { scheduleFastPurge(it) }
    }

    private fun purgeUnverifiedMembers() {
        Main.jda.guilds.forEach { guild ->
            val config = ConfigService.getGuildConfigBlocking(guild.idLong)
            val roleId = config.verificationRoleId ?: return@forEach
            val days = config.purgeUnverifiedAfterDays
            if (days <= 0) {
                return@forEach
            }

            val role = guild.getRoleById(roleId) ?: return@forEach

            val cutoff = OffsetDateTime.now().minusDays(days.toLong())
            val adminRoleIds = config.adminRoleIds
            val members = runCatching { guild.loadMembers().get() }.getOrNull()
                ?: return@forEach

            members
                .asSequence()
                .filter { shouldKickMember(it, guild.ownerIdLong, role.idLong, adminRoleIds, cutoff) }
                .forEach { member ->
                    guild.kick(member).reason("Unverified for $days+ days").queue()
                }
        }
    }

    private fun shouldKickMember(
        member: Member,
        ownerId: Long,
        verificationRoleId: Long,
        adminRoleIds: Set<Long>,
        cutoff: OffsetDateTime
    ): Boolean {
        if (member.user.isBot) {
            return false
        }
        if (member.idLong == ownerId) {
            return false
        }
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return false
        }
        if (member.roles.any { it.idLong == verificationRoleId }) {
            return false
        }
        if (member.roles.any { adminRoleIds.contains(it.idLong) }) {
            return false
        }
        if (member.roles.isNotEmpty()) {
            return false
        }

        return member.timeJoined.isBefore(cutoff)
    }

    private fun scheduleFastPurge(member: Member) {
        val guild = member.guild
        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        if (config.purgeUnverifiedAfterDays != -1) {
            return
        }

        val key = MemberKey(guild.idLong, member.idLong)
        fastPurgeTasks.remove(key)?.cancel(false)
        fastPurgeTasks[key] = scheduler.schedule({
            try {
                runFastPurge(key)
            } finally {
                fastPurgeTasks.remove(key)
            }
        }, FAST_PURGE_SECONDS, TimeUnit.SECONDS)
    }

    private fun runFastPurge(key: MemberKey) {
        val guild = Main.jda.getGuildById(key.guildId) ?: return
        val config = ConfigService.getGuildConfigBlocking(guild.idLong)
        if (config.purgeUnverifiedAfterDays != -1) {
            return
        }

        val verificationRoleId = config.verificationRoleId ?: return
        val role = guild.getRoleById(verificationRoleId) ?: return
        val member = runCatching { guild.retrieveMemberById(key.userId).complete() }.getOrNull() ?: return
        val cutoff = OffsetDateTime.now().minusSeconds(FAST_PURGE_SECONDS)

        if (!shouldKickMember(member, guild.ownerIdLong, role.idLong, config.adminRoleIds, cutoff)) {
            return
        }

        guild.kick(member).reason("Unverified for ${FAST_PURGE_SECONDS}+ seconds").queue()
    }

    private data class MemberKey(val guildId: Long, val userId: Long)
}
