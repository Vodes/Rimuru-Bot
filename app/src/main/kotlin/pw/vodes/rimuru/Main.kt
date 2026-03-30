package pw.vodes.rimuru

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import pw.vodes.rimuru.command.CommandCollection
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.listeners.ListenerMembers
import pw.vodes.rimuru.listeners.ListenerMessages
import pw.vodes.rimuru.listeners.ListenerSlashAutoComplete
import pw.vodes.rimuru.listeners.ListenerGuilds
import pw.vodes.rimuru.listeners.ListenerSlashInteraction
import pw.vodes.rimuru.services.styx.StyxProfileService
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

    UnverifiedPurgeService.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        UnverifiedPurgeService.shutdown()
        VerificationService.shutdown()
        ConfigService.shutdown()
    })
}

private fun requireBotToken(): String {
    return System.getenv("DISCORD_TOKEN")?.trim()?.takeIf { it.isNotEmpty() }
        ?: error("Missing DISCORD_TOKEN environment variable.")
}
