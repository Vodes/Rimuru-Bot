package pw.vodes.rimurukt

import org.javacord.api.entity.Icon
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.interaction.SlashCommandInteraction
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater
import java.net.URL
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
    return equals(other, true)
}

infix fun String?.ctI(other: String?): Boolean {
    if (this == null || other == null)
        return false
    return contains(other, true)
}

fun String.capitalize(): String {
    return lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun String.lightEscapeURL(): String {
    return this.replace("\"", "%22")
        .replace(" ", "%20")
        .replace("[", "%5B")
        .replace("]", "%5D")
        .replace("|", "%7C")
}

fun Icon.getFixedURL(): String {
    return if (isAnimated) url.toString().replace(".png", ".gif", true) else url.toString()
}

fun EmbedBuilder.addIconAsImage(icon: Icon, allowStream: Boolean): EmbedBuilder {
    val url = icon.getFixedURL()
    val isGif = url.contains("gif", true)
    if (!allowStream)
        return setImage(url)
    return try {
        setImage(URL(url).openStream(), if (isGif) "gif" else "png")
    } catch (_: Exception) {
        setImage(url)
    }
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