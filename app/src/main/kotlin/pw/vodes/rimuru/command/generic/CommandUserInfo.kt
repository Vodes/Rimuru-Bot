package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import java.awt.Color

class CommandUserInfo : Command("userinfo", CommandType.EVERYONE, "Show avatar and account details") {

    override fun createCommand() = slashCommand()
        .addOption(OptionType.USER, "user", "User to inspect", false)

    override fun run(event: SlashCommandInteractionEvent) {
        val target = event.getOption("user")?.asUser ?: event.user
        val targetMember = event.guild?.getMember(target)
        val mainAvatarUrl = target.effectiveAvatar.getUrl(4096)
        val serverAvatarUrl = targetMember?.effectiveAvatar?.getUrl(4096)
        val hasDistinctServerAvatar = serverAvatarUrl != null && !serverAvatarUrl.equals(mainAvatarUrl, ignoreCase = true)

        val embed = EmbedBuilder()
            .setColor(Color(0x58B98A))
            .setAuthor(target.effectiveName, null, target.effectiveAvatarUrl)
            .addField("Created", "<t:${target.timeCreated.toEpochSecond()}:R>", true)
            .setFooter("ID: ${target.id}")

        if (targetMember != null) {
            embed.addField("Joined", "<t:${targetMember.timeJoined.toEpochSecond()}:R>", true)
        }

        if (hasDistinctServerAvatar) {
            embed.addField("Main Avatar", "[Open]($mainAvatarUrl)", true)
        }
        embed.setImage(if (hasDistinctServerAvatar) serverAvatarUrl else mainAvatarUrl)

        event.replyEmbeds(embed.build()).setEphemeral(true).queue()
    }
}
