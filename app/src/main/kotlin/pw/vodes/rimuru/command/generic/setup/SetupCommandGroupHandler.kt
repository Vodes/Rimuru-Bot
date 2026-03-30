package pw.vodes.rimuru.command.generic.setup

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface SetupCommandGroupHandler {
    fun handle(event: SlashCommandInteractionEvent, guild: Guild, subcommand: String)
}
