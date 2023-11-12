package pw.vodes.rimurukt

import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.interaction.SlashCommandInteraction
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun Instant.getRelativeTimestamp(): String {
    return "<t:${this.epochSecond}:R>"
}

fun Instant.getAbsoluteTimestamp(): String {
    return "<t:${this.epochSecond}:d> <t:${this.epochSecond}:t>"
}

infix fun String?.eqI(other: String?): Boolean {
    return this.equals(other, true)
}

fun String.capitalize(): String {
    return lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}


fun CompletableFuture<Message>.deleteAfter(value: Long, unit: TimeUnit = TimeUnit.SECONDS) {
    this.thenAccept {
        it.deleteAfter(value, unit)
    }
}

fun SlashCommandInteraction.reply(content: String, ephemeral: Boolean = false): CompletableFuture<InteractionOriginalResponseUpdater> {
    val responder = this.createImmediateResponder().setContent(content)
    if (ephemeral)
        responder.setFlags(MessageFlag.EPHEMERAL)
    return responder.respond()
}