package pw.vodes.rimurukt.components

import kotlinx.coroutines.delay
import org.javacord.api.entity.message.MessageBuilder
import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.entity.message.MessageUpdater
import org.javacord.api.entity.message.component.ActionRow
import org.javacord.api.entity.message.component.Button
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.user.User
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommandInteraction
import org.javacord.api.listener.interaction.ButtonClickListener
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.launchThreaded
import java.util.concurrent.TimeUnit

class MultiPageEmbed(private val user: User, private val ownerOnly: Boolean = false, private val ephemeral: Boolean = false, first: (EmbedBuilder) -> EmbedBuilder) {

    private val pages = mutableListOf<EmbedBuilder>()
    private var current = 0

    fun addPage(page: (EmbedBuilder) -> EmbedBuilder): MultiPageEmbed {
        pages.add(page(EmbedBuilder()))
        return this
    }

    fun addPage(page: EmbedBuilder): MultiPageEmbed {
        pages.add(page)
        return this
    }

    init {
        pages.add(first(EmbedBuilder()))
    }

    private fun getClickListener(prevID: String, nextID: String, update: () -> Unit): ButtonClickListener {
        return ButtonClickListener {
            if (it.buttonInteraction.applicationId != Main.api.yourself.id)
                return@ButtonClickListener

            if (ownerOnly && !it.buttonInteraction.user.isBotOwner)
                return@ButtonClickListener

            if (it.buttonInteraction.user.id != user.id)
                return@ButtonClickListener

            var switched = false

            when (it.buttonInteraction.customId) {
                prevID -> {
                    if (current > 0)
                        current--.also { switched = true }
                }

                nextID -> {
                    if (current < pages.size - 1)
                        current++.also { switched = true }
                }

                else -> return@ButtonClickListener
            }
            it.buttonInteraction.createImmediateResponder().respond()
            if (switched)
                update()
        }
    }

    fun sendAsResponse(interaction: SlashCommandInteraction) {
        val embed = pages[current].setFooter("Page %02d / %02d".format(current + 1, pages.size))
        val (prevID, nextID) = arrayOf("prev-${interaction.id}", "next-${interaction.id}")
        val response = interaction.createImmediateResponder()
            .addComponents(
                ActionRow.of(
                    Button.primary(prevID, "⬅"),
                    Button.primary(nextID, "➡")
                )
            )
            .addEmbed(embed)

        if (ephemeral)
            response.setFlags(MessageFlag.EPHEMERAL)
        val updater = response.respond().join()

        val listener = getClickListener(prevID, nextID) {
            updater.removeAllEmbeds().addEmbed(pages[current].setFooter("Page %02d / %02d".format(current + 1, pages.size))).update()
        }
        Main.api.addButtonClickListener(listener).removeAfter(60, TimeUnit.SECONDS)

        launchThreaded {
            delay(60000)
            updater.removeAllComponents().update()
        }
    }

    fun sendMessage(event: MessageCreateEvent) {
        val embed = pages[current].setFooter("Page %02d / %02d".format(current + 1, pages.size))
        val (prevID, nextID) = arrayOf("prev-${event.messageId}", "next-${event.messageId}")
        val prevButton = Button.primary(prevID, "⬅")
        val nextButton = Button.primary(nextID, "➡")
        val message = MessageBuilder().setEmbed(embed).addActionRow(prevButton, nextButton).send(event.channel).get()
        message.addButtonClickListener(getClickListener(prevID, nextID) {
            MessageUpdater(message).setEmbed(pages[current].setFooter("Page %02d / %02d".format(current + 1, pages.size))).applyChanges()
        }).removeAfter(60, TimeUnit.SECONDS)
        launchThreaded {
            delay(60000)
            MessageUpdater(message).removeAllComponents().applyChanges()
        }
    }

}