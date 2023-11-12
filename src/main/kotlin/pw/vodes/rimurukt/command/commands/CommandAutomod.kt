package pw.vodes.rimurukt.command.commands

import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.interaction.*
import pw.vodes.rimurukt.capitalize
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.CommandType
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.reply
import pw.vodes.rimurukt.services.AutoMod
import pw.vodes.rimurukt.services.AutoModEntry
import pw.vodes.rimurukt.services.Punishment
import kotlin.jvm.optionals.getOrNull

class CommandAutomod : Command("Automod", arrayOf("filter", "automod"), CommandType.MOD, "automod") {
    override fun getSlashCommandBuilder(): SlashCommandBuilder? {
        val builder = SlashCommandBuilder()
            .setEnabledInDms(false)
            .setName(slashCommandName)
            .setDescription("Add, remove or list filters.")

        builder.addOption(
            SlashCommandOption.createWithOptions(
                SlashCommandOptionType.SUB_COMMAND, "add", "Add new filter", listOf(
                    SlashCommandOption.createStringOption("Filter", "String or Regex to be filtered", true),
                    SlashCommandOption.createWithChoices(
                        SlashCommandOptionType.STRING, "Punishment", "Type of punishment", true, listOf(
                            SlashCommandOptionChoice.create("None", "None"),
                            SlashCommandOptionChoice.create("Kick", "kick"),
                            SlashCommandOptionChoice.create("Ban", "ban"),
                            SlashCommandOptionChoice.create("Timeout", "timeout")
                        )
                    ),
                    SlashCommandOption.createLongOption("Timeout-Length", "The length of timeout in minutes. Defaults to 1.", false)
                )
            )
        )

        if (AutoMod.automods.isNotEmpty())
            builder.addOption(
                SlashCommandOption.createWithOptions(
                    SlashCommandOptionType.SUB_COMMAND, "remove", "Remove filter", listOf(
                        SlashCommandOption.createWithChoices(
                            SlashCommandOptionType.LONG, "Filter", "Filter to remove", true,
                            AutoMod.automods.mapIndexed { index, entry ->
                                SlashCommandOptionChoice.create(entry.filteredSequence, index.toLong())
                            }
                        )
                    )
                )
            )

        builder.addOption(SlashCommandOption.createSubcommand("list", "List current filters"))

        return builder
    }

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        val option = interaction.options.getOrNull(0)
        if (option == null) {
            interaction.reply("What, how?", true)
            return
        }
        when (option.name) {
            "add" -> {
                AutoMod.automods.add(
                    AutoModEntry(
                        option.options[0].stringValue.get(),
                        Punishment.get(option.options[1].stringValue.get()),
                        option.options.getOrNull(2)?.longValue?.getOrNull() ?: 1
                    )
                )
                AutoMod.save()
                Commands.updateSlashCommands()
                interaction.reply("Filter added.")
            }

            "remove" -> {
                AutoMod.automods.removeAt(option.options[0].longValue.get().toInt())
                AutoMod.save()
                Commands.updateSlashCommands()
                interaction.reply("Filter removed.")
            }

            "list" -> interaction.createImmediateResponder().addEmbed(listEmbed()).setFlags(MessageFlag.EPHEMERAL).respond()

            else -> interaction.createImmediateResponder().setContent("Somehow passed a non valid action?").setFlags(MessageFlag.EPHEMERAL).respond()
        }
    }

    private fun listEmbed(): EmbedBuilder {
        val embed = EmbedBuilder().setTitle("Filters")
        if (AutoMod.automods.isEmpty())
            embed.setDescription("There are currently no filters.")
        else
            AutoMod.automods.forEach {
                embed.addField(
                    it.filteredSequence,
                    if (it.punishment == Punishment.TIMEOUT)
                        "Timeout (${it.punishmentVal} min)"
                    else
                        it.punishment?.name?.capitalize() ?: "Remove only"
                )
            }
        return embed
    }
}