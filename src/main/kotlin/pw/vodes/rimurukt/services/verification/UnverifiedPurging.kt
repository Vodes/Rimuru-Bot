package pw.vodes.rimurukt.services.verification

import kotlinx.coroutines.time.delay
import org.javacord.api.entity.user.User
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.launchGlobal
import pw.vodes.rimurukt.services.AuditLogs
import pw.vodes.rimurukt.services.StaffAction
import pw.vodes.rimurukt.services.StaffActionType
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit


object UnverifiedPurging {
    fun start() {
        launchGlobal {
            while (Main.config.purge3Days) {
                val purgeTime = Instant.now().minus(3, ChronoUnit.DAYS)
                val purgeTimeNewUser = Instant.now().minus(24, ChronoUnit.HOURS)
                for (user in Main.server.members) {
                    if (!shouldKick(user))
                        continue
                    val isNewUser = user.creationTimestamp.isAfter(Instant.now().minus(7, ChronoUnit.DAYS))
                    if (user.getJoinedAtTimestamp(Main.server).get().isBefore(if (isNewUser) purgeTimeNewUser else purgeTime)) {
                        Main.server.kickUser(user, "Unverified for 3+ days").thenRun {
                            AuditLogs.registerStaffAction(
                                StaffAction(
                                    user.idAsString,
                                    Main.api.yourself.idAsString,
                                    Instant.now().epochSecond,
                                    StaffActionType.KICK,
                                    "Unverified for 3+ days"
                                )
                            )
                        }
                    }
                }
                delay(Duration.ofMinutes(10))
            }
        }
    }

    private fun shouldKick(user: User): Boolean {
        return !(user.isBotOwner || user.isBot || Main.server.isAdmin(user) || Main.server.getRoles(user).filterNot { it.isEveryoneRole }
            .isNotEmpty())
    }
}
