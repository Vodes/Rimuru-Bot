package pw.vodes.rimuru

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.GatewayIntent
import pw.vodes.rimuru.command.CommandCollection
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.listeners.*
import pw.vodes.rimuru.services.logging.MemberLogService
import pw.vodes.rimuru.services.rss.RssFeedService
import pw.vodes.rimuru.services.styx.StyxProfileService
import pw.vodes.rimuru.services.styx.StyxVotingService
import pw.vodes.rimuru.services.verification.UnverifiedPurgeService
import pw.vodes.rimuru.services.verification.VerificationService

object Main {
    lateinit var jda: JDA
}

fun main() {
    ConfigService.initBlocking()

    Main.jda = JDABuilder.createDefault(requireBotToken(), GatewayIntent.entries)
        .addEventListeners(ListenerMessages(), ListenerMembers(), ListenerGuilds())
        .addEventListeners(ListenerSlashInteraction(), ListenerSlashAutoComplete())
        .build()

    Main.jda.awaitReady()
    println("Invite link: ${Main.jda.getInviteUrl(Permission.ADMINISTRATOR)}")
    StyxProfileService.applyConfiguredProfiles(Main.jda.guilds)

    val commands = CommandCollection.commands.map { it.createCommand() }

    // Clear legacy guild-scoped commands so only the current global set remains.
    Main.jda.guilds.forEach { guild ->
        guild.updateCommands().complete()
    }

    // Bulk overwrite global commands with the current command collection.
    Main.jda.updateCommands()
        .addCommands(commands)
        .complete()

    RssFeedService.start()
    UnverifiedPurgeService.start()
    StyxVotingService.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        MemberLogService.shutdown()
        RssFeedService.shutdown()
        UnverifiedPurgeService.shutdown()
        StyxVotingService.shutdown()
        VerificationService.shutdown()
        ConfigService.shutdown()
    })
}

private fun requireBotToken(): String {
    return System.getenv("DISCORD_TOKEN")?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("Missing DISCORD_TOKEN environment variable.")
}
