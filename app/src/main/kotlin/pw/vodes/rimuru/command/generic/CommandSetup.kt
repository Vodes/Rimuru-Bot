package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import pw.vodes.rimuru.command.generic.setup.*

class CommandSetup : Command("setup", CommandType.ADMIN, "Configure this server") {
    private val handlers: Map<String, SetupCommandGroupHandler> = mapOf(
        "adminrole" to SetupAdminRoleGroup,
        "verification" to SetupVerificationGroup,
        "autorole" to SetupAutoroleGroup,
        "logging" to SetupLoggingGroup
    )

    override fun guildOnly() = true

    override fun createCommand() = slashCommand()
        .addSubcommandGroups(
            SubcommandGroupData("adminrole", "Manage roles allowed to use setup")
                .addSubcommands(
                    SubcommandData("add", "Allow a role to use setup").addOption(OptionType.ROLE, "role", "Role to allow", true),
                    SubcommandData("remove", "Remove setup access from a role").addOption(
                        OptionType.ROLE,
                        "role",
                        "Role to remove",
                        true
                    ),
                    SubcommandData("list", "List all setup admin roles")
                ),
            SubcommandGroupData("verification", "Configure verification flow")
                .addSubcommands(
                    SubcommandData("configure", "Set verification role/channel/message")
                        .addOption(OptionType.ROLE, "role", "Role assigned after successful verification", true)
                        .addOption(OptionType.CHANNEL, "channel", "Verification text channel", true)
                        .addOption(OptionType.STRING, "message", "Optional existing verification message link", false),
                    SubcommandData("delay", "Set unverified kick delay in days (0 disables auto-kick)")
                        .addOption(
                            OptionType.INTEGER,
                            "days",
                            "Days before unverified users are kicked. Use 0 to disable or -1 for 30-second fast purge mode",
                            true
                        ),
                    SubcommandData("hallofshame", "Set or clear the failed verification channel")
                        .addOption(
                            OptionType.CHANNEL,
                            "channel",
                            "Channel for failed verification embeds; omit to disable",
                            false
                        ),
                    SubcommandData("disable", "Disable verification flow"),
                    SubcommandData("status", "Show current verification settings")
                ),
            SubcommandGroupData("autorole", "Configure reaction autoroles")
                .addSubcommands(
                    SubcommandData("add", "Add an autorole for reactions on a message")
                        .addOption(OptionType.STRING, "message", "Message link to watch for reactions", true)
                        .addOption(OptionType.ROLE, "role", "Role to add/remove on reaction add/remove", true),
                    SubcommandData("remove", "Open selector to remove an autorole entry"),
                    SubcommandData("list", "List configured autorole entries")
                ),
            SubcommandGroupData("logging", "Configure guild logging channels")
                .addSubcommands(
                    SubcommandData("user", "Set or clear the user join/leave logging channel")
                        .addOptions(loggingChannelOption()),
                    SubcommandData("exception", "Set or clear the exception logging channel")
                        .addOptions(loggingChannelOption()),
                    SubcommandData("status", "Show current logging channel settings")
                )
        )

    override fun run(event: SlashCommandInteractionEvent) {
        val guildContext = requireGuildContext(event, requireConfiguredAdmin = true) ?: return
        if (!guildContext.guild.selfMember.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("`/setup` requires me to have `Administrator` permission in this server.")
                .setEphemeral(true)
                .queue()
            return
        }

        val group = event.subcommandGroup
        val subcommand = event.subcommandName
        if (group == null || subcommand == null) {
            event.reply("Missing setup subcommand.").setEphemeral(true).queue()
            return
        }

        handlers[group]?.handle(event, guildContext.guild, subcommand)
            ?: event.reply("Unknown setup group.").setEphemeral(true).queue()
    }

    private fun loggingChannelOption(): OptionData {
        return OptionData(OptionType.CHANNEL, "channel", "Message channel to use; omit to disable", false)
            .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
    }
}
