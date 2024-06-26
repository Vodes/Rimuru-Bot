package pw.vodes.rimurukt

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.activity.ActivityType
import org.javacord.api.entity.server.Server
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.misc.MemberListeners
import pw.vodes.rimurukt.misc.Updater
import pw.vodes.rimurukt.services.AuditLogs
import pw.vodes.rimurukt.services.AutoMod
import pw.vodes.rimurukt.services.AutoRoles
import pw.vodes.rimurukt.services.rss.RSSFeeds
import pw.vodes.rimurukt.services.verification.UnverifiedPurging
import pw.vodes.rimurukt.services.verification.VerificationListener
import java.io.File
import kotlin.jvm.optionals.getOrNull
import kotlin.system.exitProcess

object Main {
    lateinit var config: Config
    lateinit var api: DiscordApi
    lateinit var server: Server
    lateinit var appDir: File

    lateinit var configFile: File
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
    Main.server.addMessageEditListener(AutoMod.automodEditListener())

    AuditLogs.start()
    Main.server.addServerMemberJoinListener(MemberListeners.JoinListener())
    Main.server.addServerMemberLeaveListener(MemberListeners.LeaveListener())

    RSSFeeds.load()

    Commands.load()
    Main.server.addMessageCreateListener { Commands.tryRunCommand(it) }
    Commands.save()

    UnverifiedPurging.start()
    if (Main.config.verificationChannel.isNotBlank()) {
        val verificationChannel = Main.server.getChannelById(Main.config.verificationChannel).getOrNull() ?: return
        val verificationRole = Main.server.getRoleById(Main.config.verificationRole).getOrNull() ?: return
        val verificationMessage =
            verificationChannel.asServerTextChannel().getOrNull()?.getMessageById(Main.config.verificationReactionMessage)?.get() ?: return
        verificationMessage.addReactionAddListener(VerificationListener())
    }
}

