package pw.vodes.rimuru.util

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

object InteractionDispatcher {
    private val stringSelectHandlers = linkedSetOf<StringSelectHandler>()
    private val modalHandlers = linkedSetOf<ModalHandler>()

    fun registerStringSelect(handler: StringSelectHandler) {
        stringSelectHandlers += handler
    }

    fun registerModal(handler: ModalHandler) {
        modalHandlers += handler
    }

    fun onStringSelect(event: StringSelectInteractionEvent) {
        val handler = stringSelectHandlers.firstOrNull { it.matches(event.componentId) } ?: return
        handler.handle(event)
    }

    fun onModal(event: ModalInteractionEvent) {
        val handler = modalHandlers.firstOrNull { it.matches(event.modalId) } ?: return
        handler.handle(event)
    }

    interface StringSelectHandler {
        fun matches(componentId: String): Boolean
        fun handle(event: StringSelectInteractionEvent)
    }

    interface ModalHandler {
        fun matches(modalId: String): Boolean
        fun handle(event: ModalInteractionEvent)
    }
}
