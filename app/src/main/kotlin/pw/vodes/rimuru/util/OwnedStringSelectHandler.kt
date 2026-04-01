package pw.vodes.rimuru.util

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

abstract class OwnedStringSelectHandler(
    private val prefix: String,
    private val missingSelectionMessage: String,
    private val invalidGuildMessage: String = "This selector is no longer valid in this server.",
    private val invalidUserMessage: String = "Only the user who opened this selector can use it.",
    private val missingGuildMessage: String = "This action can only be used in a server."
) : InteractionDispatcher.StringSelectHandler {
    init {
        InteractionDispatcher.registerStringSelect(this)
    }

    final override fun matches(componentId: String): Boolean {
        return ScopedInteractionIds.parse(componentId, prefix) != null
    }

    final override fun handle(event: StringSelectInteractionEvent) {
        val context = ScopedInteractionIds.parse(event.componentId, prefix) ?: return
        val selectedValue = event.values.firstOrNull() ?: run {
            event.reply(missingSelectionMessage).setEphemeral(true).queue()
            return
        }
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

        onSelection(event, guild, selectedValue, context)
    }

    protected fun createMenuId(guildId: Long, userId: Long, vararg extraParts: String): String {
        return ScopedInteractionIds.create(prefix, guildId, userId, *extraParts)
    }

    protected fun replyWithOwnedMenu(
        event: SlashCommandInteractionEvent,
        guildId: Long,
        userId: Long,
        prompt: String,
        placeholder: String,
        options: List<SelectOption>,
        maxOptions: Int = DEFAULT_MAX_OPTIONS,
        overflowSuffix: ((total: Int, shown: Int) -> String)? = null
    ) {
        val shownOptions = options.take(maxOptions)
        val suffix = if (options.size > shownOptions.size && overflowSuffix != null) {
            overflowSuffix(options.size, shownOptions.size)
        } else {
            ""
        }

        val menu = StringSelectMenu.create(createMenuId(guildId, userId))
            .setPlaceholder(placeholder)
            .setRequiredRange(1, 1)
            .addOptions(shownOptions)
            .build()

        event.reply(prompt + suffix)
            .addComponents(ActionRow.of(menu))
            .setEphemeral(true)
            .queue()
    }

    protected abstract fun onSelection(
        event: StringSelectInteractionEvent,
        guild: Guild,
        selectedValue: String,
        context: ScopedInteractionId
    )

    companion object {
        const val DEFAULT_MAX_OPTIONS = 25
    }
}
