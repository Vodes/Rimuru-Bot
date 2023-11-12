package pw.vodes.rimurukt.command.commands

import org.javacord.api.entity.message.MessageFlag
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.server.Server
import org.javacord.api.interaction.*
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.command.Commands
import pw.vodes.rimurukt.eqI
import pw.vodes.rimurukt.reply
import pw.vodes.rimurukt.services.AutoRoles
import kotlin.jvm.optionals.getOrNull

class CommandAutorole : Command("autorole", slashCommandName = "autorole") {

    override fun getSlashCommandBuilder(): SlashCommandBuilder? {
        val builder = SlashCommandBuilder()
            .setEnabledInDms(false)
            .setName(slashCommandName)
            .setDescription("Add, remove or list autoroles.")

        builder.addOption(
            SlashCommandOption.createWithOptions(
                SlashCommandOptionType.SUB_COMMAND, "add", "Add new autorole", listOf(
                    SlashCommandOption.createStringOption("Message", "The message that will be listened to for reactions", true),
                    SlashCommandOption.createRoleOption("Role", "The role that will be applied/removed.", true)
                )
            )
        )

        if (AutoRoles.autoroles.isNotEmpty())
            builder.addOption(
                SlashCommandOption.createWithOptions(
                    SlashCommandOptionType.SUB_COMMAND, "remove", "Remove autorole", listOf(
                        SlashCommandOption.createWithChoices(
                            SlashCommandOptionType.LONG, "Autorole", "Autorole to remove", true,
                            AutoRoles.autoroles.mapIndexed { index, ar ->
                                val role = ar.role()?.name ?: "Unknown Role"
                                val channel = ar.channel()?.name ?: "Unknown Channel"
                                SlashCommandOptionChoice.create("$role (in #$channel)", index.toLong())
                            }
                        )
                    )
                )
            )

        builder.addOption(SlashCommandOption.createSubcommand("list", "List current autoroles"))

        return builder
    }

    override fun runSlashCommand(interaction: SlashCommandInteraction) {
        val opt = interaction.options.getOrNull(0)
        if (opt == null) {
            interaction.reply("What, how?", true)
            return
        }
        when (opt.name) {
            "add" -> {
                val msgOpt = Main.api.getMessageByLink(opt.getOptionByName("Message").get().stringValue.get())
                if (msgOpt.isEmpty)
                    interaction.reply("Your linked message was not found/is invalid.", true).also { return }

                val msg = msgOpt.get().get()

                if (msg.server.isEmpty)
                    interaction.reply("Your linked message is not on a server.", true).also { return }

                val roleOpt = opt.getOptionByName("Role").get().roleValue
                if (roleOpt.isEmpty || roleOpt.get().server.id != msg.server.get().id)
                    interaction.reply("Your given role was not found/is invalid.", true).also { return }

                val result = AutoRoles.addAutoRole(msg.server.get().idAsString, msg.channel.idAsString, msg.idAsString, roleOpt.get().idAsString)
                if (!result)
                    interaction.reply("Could not add listeners. Please check the logs.", true).also { return }

                Commands.updateSlashCommands()
                interaction.reply("Autorole added.")
            }

            "remove" -> {
                AutoRoles.removeAutoRole(AutoRoles.autoroles[opt.options[0].longValue.get().toInt()])
                Commands.updateSlashCommands()
                interaction.reply("Autorole removed.")
            }

            "list" -> interaction.createImmediateResponder().addEmbed(listEmbed(interaction.server.get())).setFlags(MessageFlag.EPHEMERAL).respond()

            else -> interaction.createImmediateResponder().setContent("Somehow passed a non valid action?").setFlags(MessageFlag.EPHEMERAL).respond()
        }
    }

    private fun listEmbed(server: Server): EmbedBuilder {
        val embed = EmbedBuilder().setTitle("Autoroles")

        val autoroles = AutoRoles.autoroles.filter { it.serverID eqI server.idAsString }

        if (autoroles.isEmpty())
            embed.setDescription("There are currently no autoroles.")
        else
            autoroles.forEachIndexed { index, ar ->
                val role = server.getRoleById(ar.roleID).getOrNull()?.name ?: "Unknown/Invalid Role"
                embed.addField("${index + 1}. $role", ar.message()?.link.toString())
            }
        return embed
    }
}