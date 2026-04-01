package pw.vodes.rimuru.util

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice

object AutocompleteChoices {
    const val DEFAULT_MAX_CHOICES = 25

    fun <T> replyMatching(
        event: CommandAutoCompleteInteractionEvent,
        entries: Iterable<T>,
        maxChoices: Int = DEFAULT_MAX_CHOICES,
        matches: (entry: T, query: String) -> Boolean,
        toChoice: (entry: T) -> Choice
    ) {
        val query = event.focusedOption.value.lowercase()
        val choices = entries
            .filter { entry -> matches(entry, query) }
            .take(maxChoices)
            .map(toChoice)

        event.replyChoices(choices).queue()
    }
}
