package pw.vodes.rimuru.command.generic

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import pw.vodes.rimuru.util.DiscordMessageLinks
import kotlin.concurrent.thread

class CommandClear : Command("clear", CommandType.MOD, "Delete messages by amount or from a message link") {

    override fun requiredGuildPermissions() = arrayOf(Permission.MESSAGE_MANAGE)
    override fun guildOnly() = true

    override fun createCommand() = slashCommand()
        .addOption(OptionType.INTEGER, "amount", "Delete this many recent messages in the current channel", false)
        .addOption(OptionType.STRING, "message", "Message link; deletes that message and all newer ones", false)

    override fun run(event: SlashCommandInteractionEvent) {
        val guildContext = requireGuildContext(event) ?: return
        val guild = guildContext.guild
        val member = guildContext.member

        val amount = event.getOption("amount")?.asLong?.toInt()
        val messageLink = event.getOption("message")?.asString?.trim().orEmpty()

        if ((amount == null && messageLink.isBlank()) || (amount != null && messageLink.isNotBlank())) {
            event.reply("Provide either `amount` or `message`, but not both.").setEphemeral(true).queue()
            return
        }
        if (amount != null && amount !in 1..1000) {
            event.reply("`amount` must be between 1 and 1000.").setEphemeral(true).queue()
            return
        }

        event.deferReply(true).queue()
        thread(start = true, isDaemon = true) {
            val response = try {
                if (amount != null) {
                    clearByAmount(event, guild, member, amount)
                } else {
                    clearByMessageLink(event, guild, member, messageLink)
                }
            } catch (e: Exception) {
                "Failed to clear messages: ${e.message ?: "unknown error"}"
            }
            event.hook.editOriginal(response).queue()
        }
    }

    private fun clearByAmount(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        member: Member,
        amount: Int
    ): String {
        val channel = guild.getChannelById(GuildMessageChannel::class.java, event.channel.idLong)
            ?: return "This command can only be used in a guild message channel."
        if (!member.hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            return "You don't have permission to manage messages in ${channel.asMention}."
        }

        val messages = collectRecentMessages(channel, amount)
        if (messages.isEmpty()) {
            return "No messages found to delete."
        }

        val result = deleteMessages(messages)
        return "Deleted ${result.deleted}/${messages.size} message(s) in ${channel.asMention}${result.failSuffix()}."
    }

    private fun clearByMessageLink(
        event: SlashCommandInteractionEvent,
        guild: Guild,
        member: Member,
        link: String
    ): String {
        val parsedLink = DiscordMessageLinks.parse(link)
            ?: return "Invalid message link."
        if (parsedLink.isDm) {
            return "Message link must point to this server."
        }

        if (parsedLink.guildIdLong != guild.idLong) {
            return "Message link must point to this server."
        }

        val channel = guild.getChannelById(GuildMessageChannel::class.java, parsedLink.channelId)
            ?: return "Could not resolve the channel from that message link."

        if (!member.hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            return "You don't have permission to manage messages in ${channel.asMention}."
        }

        try {
            channel.retrieveMessageById(parsedLink.messageId).complete()
        } catch (_: Exception) {
            return "Could not retrieve the target message from that link."
        }

        val messages = collectMessagesAfter(channel, parsedLink.messageId)
        if (messages.isEmpty()) {
            return "No messages found to delete."
        }

        val result = deleteMessages(messages)
        return "Deleted ${result.deleted}/${messages.size} message(s) in ${channel.asMention}${result.failSuffix()}."
    }

    private fun collectRecentMessages(channel: GuildMessageChannel, amount: Int): List<Message> {
        val messages = mutableListOf<Message>()
        val iterator = channel.iterableHistory.iterator()
        while (iterator.hasNext() && messages.size < amount) {
            messages.add(iterator.next())
        }
        return messages
    }

    private fun collectMessagesAfter(channel: GuildMessageChannel, messageId: Long): List<Message> {
        val messages = mutableListOf<Message>()
        val iterator = channel.iterableHistory.iterator()

        while (iterator.hasNext()) {
            val message = iterator.next()
            messages.add(message)
            if (message.idLong == messageId) {
                break
            }
        }

        return messages
    }

    private fun deleteMessages(messages: List<Message>): DeletionResult {
        var deleted = 0
        var failed = 0
        messages.forEach { message ->
            try {
                message.delete().complete()
                deleted++
            } catch (_: Exception) {
                failed++
            }
        }
        return DeletionResult(deleted, failed)
    }

    private data class DeletionResult(val deleted: Int, val failed: Int) {
        fun failSuffix(): String = if (failed == 0) "" else " (${failed} failed)"
    }

}
