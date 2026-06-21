package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType

class CommandPing : Command("ping", CommandType.EVERYONE, "Check bot latency") {
    override fun createCommand() = slashCommand()

    override fun run(event: SlashCommandInteractionEvent) {
        val gatewayPing = event.jda.gatewayPing
        event.jda.restPing.queue(
            { restPing ->
                event.reply("Pong: gateway ${gatewayPing} ms | rest ${restPing} ms")
                    .setEphemeral(true)
                    .queue()
            },
            {
                event.reply("Pong: gateway ${gatewayPing} ms")
                    .setEphemeral(true)
                    .queue()
            }
        )
    }
}
