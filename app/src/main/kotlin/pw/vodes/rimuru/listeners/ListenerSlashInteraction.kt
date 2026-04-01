package pw.vodes.rimuru.listeners

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pw.vodes.rimuru.command.CommandCollection
import pw.vodes.rimuru.util.InteractionDispatcher

class ListenerSlashInteraction : ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        CommandCollection.onInteraction(event)
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        InteractionDispatcher.onStringSelect(event)
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        InteractionDispatcher.onModal(event)
    }
}
