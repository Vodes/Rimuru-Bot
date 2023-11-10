package pw.vodes.rimurukt

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.activity.ActivityType
import org.javacord.api.entity.server.Server
import pw.vodes.rimurukt.audit.AuditLogs
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.file.AutoMod
import pw.vodes.rimurukt.file.AutoRoles
import pw.vodes.rimurukt.listeners.MemberListeners
import pw.vodes.rimurukt.updater.Updater
import java.io.File
import kotlin.system.exitProcess

object Main {
    lateinit var config: Config
    lateinit var api: DiscordApi
    lateinit var server: Server
    lateinit var appDir: File

    lateinit var configFile: File

    const val VERSION = "1.0.0"
}

fun main(args: Array<String>) {
    Main.appDir = if (args.isEmpty()) getAppDir() else File(args[0]).also { it.mkdirs() }
    Main.configFile = File(Main.appDir, "config.toml")
    if (!Main.configFile.exists()) {
        Main.configFile.writeText(toml.encodeToString(Config()))
        println("Please setup your config at: ${Main.configFile.absolutePath}")
        exitProcess(1)
    }
    Main.config = toml.decodeFromString(Main.configFile.readText())
    if (Main.config.botToken.isBlank())
        println("No bot token was found in the config!").also { exitProcess(1) }

    Main.api = DiscordApiBuilder().setToken(Main.config.botToken).setAllIntents().login().join()

    if (Main.api.servers.isEmpty())
        println("The bot is not in any servers. Please invite it with this link: ${Main.api.createBotInvite()}").also { exitProcess(1) }

    Main.api.updateActivity(ActivityType.PLAYING, "${Main.config.commandPrefix}help")
    Main.server = Main.api.servers.first()
    LogRepeater.start()
    initServices()
}

fun initServices() {
    Updater.init()
    Updater.postRestart()

    AutoRoles.load()
    AutoMod.load()
    Main.server.addMessageCreateListener(AutoMod.automodCreateListener())
    //Main.server.addMessageEditListener(AutoMod.automodEditListener())

    AuditLogs.start()
    Main.server.addServerMemberJoinListener(MemberListeners.JoinListener())
    Main.server.addServerMemberLeaveListener(MemberListeners.LeaveListener())

    Commands.load()
    Main.server.addMessageCreateListener { Commands.tryRunCommand(it) }
}

