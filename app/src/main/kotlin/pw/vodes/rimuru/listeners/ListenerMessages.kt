package pw.vodes.rimuru.listeners

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pw.vodes.rimuru.services.autorole.AutoRoleService
import pw.vodes.rimuru.services.verification.VerificationService

class ListenerMessages : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot || event.author.isSystem) return
        VerificationService.onMessageReceived(event)
        // Reserved for future message-based features (logging, automod, etc).
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user?.isBot == true) return
        AutoRoleService.onReactionAdd(event)
        VerificationService.onReactionAdd(event)
    }

    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        AutoRoleService.onReactionRemove(event)
    }
}
