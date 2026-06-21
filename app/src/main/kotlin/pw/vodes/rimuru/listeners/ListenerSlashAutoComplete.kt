package pw.vodes.rimuru.listeners

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pw.vodes.rimuru.command.CommandCollection

class ListenerSlashAutoComplete : ListenerAdapter() {
    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        CommandCollection.onAutoComplete(event)
    }
}