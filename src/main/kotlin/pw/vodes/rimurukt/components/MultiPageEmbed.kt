package pw.vodes.rimurukt.components

import kotlinx.coroutines.delay
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.MessageBuilder
import org.javacord.api.entity.message.MessageUpdater
import org.javacord.api.entity.message.component.Button
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.user.User
import pw.vodes.rimurukt.launchThreaded
import java.util.concurrent.TimeUnit

class MultiPageEmbed(val user: User, val ownerOnly: Boolean, first: (EmbedBuilder) -> EmbedBuilder) {

    private val pages = mutableListOf<EmbedBuilder>()
    private var current = 0

    private lateinit var message: Message

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

    fun sendMessage(channel: TextChannel) {
        val embed = pages[current].setFooter("Page %02d / %02d".format(current + 1, pages.size))
        val prevButton = Button.primary("prev", "⬅")
        val nextButton = Button.primary("next", "➡")
        message = MessageBuilder().setEmbed(embed).addActionRow(prevButton, nextButton).send(channel).get()

        message.addButtonClickListener {
            if (ownerOnly && !it.buttonInteraction.user.isBotOwner)
                return@addButtonClickListener

            if (it.buttonInteraction.user.id != user.id)
                return@addButtonClickListener

            var switched = false

            when (it.buttonInteraction.customId) {
                "prev" -> {
                    if (current > 0)
                        current--.also { switched = true }
                }

                "next" -> {
                    if (current < pages.size - 1)
                        current++.also { switched = true }
                }

                else -> return@addButtonClickListener
            }
            it.buttonInteraction.createImmediateResponder().respond()
            if (switched)
                MessageUpdater(message).setEmbed(pages[current].setFooter("Page %02d / %02d".format(current + 1, pages.size))).applyChanges()
        }.removeAfter(60, TimeUnit.SECONDS)

        launchThreaded {
            delay(60000)
            MessageUpdater(message).removeAllComponents().applyChanges()
        }
    }

}