package pw.vodes.rimuru.util

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

abstract class OwnedModalHandler(
    private val prefix: String,
    private val invalidGuildMessage: String = "This dialog is no longer valid in this server.",
    private val invalidUserMessage: String = "Only the user who opened this dialog can use it.",
    private val missingGuildMessage: String = "This action can only be used in a server."
) : InteractionDispatcher.ModalHandler {
    init {
        InteractionDispatcher.registerModal(this)
    }

    final override fun matches(modalId: String): Boolean {
        return ScopedInteractionIds.parse(modalId, prefix) != null
    }

    final override fun handle(event: ModalInteractionEvent) {
        val context = ScopedInteractionIds.parse(event.modalId, prefix) ?: return
        if (event.guild?.idLong != context.guildId) {
            event.reply(invalidGuildMessage).setEphemeral(true).queue()
            return
        }
        if (event.user.idLong != context.userId) {
            event.reply(invalidUserMessage).setEphemeral(true).queue()
            return
        }

        val guild = event.guild ?: run {
            event.reply(missingGuildMessage).setEphemeral(true).queue()
            return
        }

        onSubmit(event, guild, context)
    }

    protected fun createModalId(guildId: Long, userId: Long, vararg extraParts: String): String {
        return ScopedInteractionIds.create(prefix, guildId, userId, *extraParts)
    }

    protected abstract fun onSubmit(
        event: ModalInteractionEvent,
        guild: Guild,
        context: ScopedInteractionId
    )
}
