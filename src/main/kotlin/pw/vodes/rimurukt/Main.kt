package pw.vodes.rimurukt

import kotlinx.serialization.encodeToString
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.activity.ActivityType
import org.javacord.api.entity.server.Server
import pw.vodes.rimurukt.audit.AuditLogs
import pw.vodes.rimurukt.file.AutoMod
import pw.vodes.rimurukt.file.AutoRoles
import pw.vodes.rimurukt.listeners.MemberListeners
import java.io.File
import kotlin.system.exitProcess

object Main {
    lateinit var config: Config
    lateinit var api: DiscordApi
    lateinit var server: Server
    lateinit var appDir: File
}

fun main(args: Array<String>) {
    Main.appDir = if (args.isEmpty()) getAppDir() else File(args[0]).also { it.mkdirs() }
    val configFile = File(Main.appDir, "config.json")
    if (!configFile.exists()) {
        val test = Config()
        configFile.writeText(json.encodeToString(test))
        println("Please setup your config at: ${configFile.absolutePath}")
        exitProcess(1)
    }
    Main.config = json.decodeFromString(configFile.readText())
    if (Main.config.botToken.isBlank())
        println("No bot token was found in the config!").also { exitProcess(1) }

    Main.api = DiscordApiBuilder().setToken(Main.config.botToken).setAllIntents().login().join()

    if (Main.api.servers.isEmpty())
        println("The bot is not in any servers. Please invite it with this link: ${Main.api.createBotInvite()}").also { exitProcess(1) }

    Main.api.updateActivity(ActivityType.PLAYING, "${Main.config.commandPrefix}help")
    Main.server = Main.api.servers.first()
    LogRepeater.start()

    AutoRoles.load()
    AutoMod.load()
    Main.server.addMessageCreateListener(AutoMod.automodCreateListener())
    Main.server.addMessageEditListener(AutoMod.automodEditListener())

    AuditLogs.start()
    Main.server.addServerMemberJoinListener(MemberListeners.JoinListener())
    Main.server.addServerMemberLeaveListener(MemberListeners.LeaveListener())

}

