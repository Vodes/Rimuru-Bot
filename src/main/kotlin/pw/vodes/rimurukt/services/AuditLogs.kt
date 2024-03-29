package pw.vodes.rimurukt.services

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import org.javacord.api.entity.auditlog.AuditLogActionType
import org.javacord.api.entity.user.User
import pw.vodes.rimurukt.*
import java.io.File
import java.net.SocketTimeoutException
import java.time.Instant
import kotlin.math.abs

enum class StaffActionType {
    KICK, BAN, UNBAN, TIMEOUT;

    fun messageTitle(): String {
        return when (this) {
            KICK -> "User kicked"
            BAN -> "User banned"
            UNBAN -> "User unbanned"
            else -> "User muted"
        }
    }
}

object AuditLogs {
    private var timeouted = false
    private val file = File(Main.appDir, "staffactions.json")
    private var job = Job()

    var staffActions = mutableListOf<StaffAction>()

    fun wasKickedOrBanned(user: User) = staffActions.find {
        it.affectedUser.equals(user.idAsString, true)
                && abs(it.time - Instant.now().epochSecond) < 7
                && it.type in arrayOf(StaffActionType.BAN, StaffActionType.KICK)
    } != null

    fun registerStaffAction(staffAction: StaffAction) {
        if (staffActions.find { it == staffAction } != null)
            return

        staffActions.add(staffAction)
        save()
        Main.config.userLogChannel()?.sendMessage(staffAction.getEmbed())
    }

    private fun save() {
        file.writeText(json.encodeToString(staffActions))
    }

    private fun load() {
        if (file.exists()) {
            staffActions = json.decodeFromString(file.readText())
        }
    }

    fun start() {
        load()
        launchGlobal {
            while (true) {
                checkAuditLogs()
                delay(3000)
            }
        }
    }

    private fun checkAuditLogs() {
        try {
            val logs = Main.server.getAuditLog(15).join().entries
            val filtered =
                logs.filter { it.creationTimestamp.isAfter(Instant.now().minusMillis(if (timeouted) 15 else 8)) && !it.user.get().isYourself }
            for (log in filtered) {
                when (log.type) {
                    AuditLogActionType.MEMBER_KICK -> {
                        registerStaffAction(
                            StaffAction(
                                log.target.get().asUser().get().idAsString,
                                log.user.get().idAsString,
                                log.creationTimestamp.epochSecond,
                                StaffActionType.KICK,
                                log.reason.orElse("")
                            )
                        )
                    }

                    AuditLogActionType.MEMBER_BAN_ADD -> {
                        registerStaffAction(
                            StaffAction(
                                log.target.get().asUser().get().idAsString,
                                log.user.get().idAsString,
                                log.creationTimestamp.epochSecond,
                                StaffActionType.BAN,
                                log.reason.orElse("")
                            )
                        )
                    }

                    AuditLogActionType.MEMBER_BAN_REMOVE -> {
                        registerStaffAction(
                            StaffAction(
                                log.target.get().asUser().get().idAsString,
                                log.user.get().idAsString,
                                log.creationTimestamp.epochSecond,
                                StaffActionType.UNBAN,
                                log.reason.orElse("")
                            )
                        )
                    }

                    else -> continue
                }
            }
            timeouted = false
        } catch (ex: Exception) {
            if (ex is SocketTimeoutException || ex.message ctI "timeout")
                timeouted = true
            else
                reportException(ex, this.javaClass.canonicalName)
        }
    }
}