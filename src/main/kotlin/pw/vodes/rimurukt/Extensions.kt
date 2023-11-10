package pw.vodes.rimurukt

import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.interaction.SlashCommandInteraction
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater
import java.time.Instant
import java.util.concurrent.CompletableFuture

fun Instant.getRelativeTimestamp(): String {
    return "<t:${this.epochSecond}:R>"
}

fun Instant.getAbsoluteTimestamp(): String {
    return "<t:${this.epochSecond}:d> <t:${this.epochSecond}:t>"
}

fun SlashCommandInteraction.reply(content: String, ephemeral: Boolean = false): CompletableFuture<InteractionOriginalResponseUpdater> {
    val responder = this.createImmediateResponder().setContent(content)
    if (ephemeral)
        responder.setFlags(MessageFlag.EPHEMERAL)
    return responder.respond()
}