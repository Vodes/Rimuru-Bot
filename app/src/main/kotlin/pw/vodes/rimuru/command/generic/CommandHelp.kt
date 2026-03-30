package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import pw.vodes.rimuru.BuildConfig
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import java.awt.Color

class CommandHelp : Command("help", CommandType.EVERYONE, "Show bot info") {

    override fun createCommand() = slashCommand()

    override fun run(event: SlashCommandInteractionEvent) {
        val selfUser = event.jda.selfUser

        val embed = EmbedBuilder()
            .setTitle(BuildConfig.APPNAME)
            .setColor(Color(0x4E9FD1))
            .setAuthor(event.user.effectiveName, null, event.user.effectiveAvatarUrl)
            .setThumbnail(selfUser.effectiveAvatar.getUrl(4096))
            .addField("Version", BuildConfig.VERSION, true)
            .addField("Written by", "<@129871096299126784>", true)
            .addField("Source code", "https://github.com/Vodes/Rimuru-Bot", false)
            .addField("Bot User", selfUser.asTag, true)
            .addField("Servers", event.jda.guilds.size.toString(), true)
            .setFooter("ID: ${selfUser.id}")

        event.replyEmbeds(embed.build()).setEphemeral(true).queue()
    }
}
