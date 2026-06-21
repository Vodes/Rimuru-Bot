package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class CommandRestart : Command("restart", CommandType.ADMIN, "Exit process so container can restart it") {

    override fun requiredGuildPermissions() = arrayOf(Permission.ADMINISTRATOR)
    override fun guildOnly() = true

    override fun createCommand() = slashCommand()

    override fun run(event: SlashCommandInteractionEvent) {
        if (requireGuildContext(event) == null) return
        val applicationOwnerId = runCatching { event.jda.retrieveApplicationInfo().complete().owner.idLong }.getOrNull()
        if (applicationOwnerId == null || event.user.idLong != applicationOwnerId) {
            event.reply("Only the application owner can restart the bot process.").setEphemeral(true).queue()
            return
        }

        event.reply("Restarting process...").setEphemeral(true).queue {
            thread(start = true, isDaemon = true) {
                Thread.sleep(1200)
                exitProcess(0)
            }
        }
    }
}
