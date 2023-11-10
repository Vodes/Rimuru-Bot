package pw.vodes.rimurukt.file

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.message.MessageAuthor
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.listener.message.MessageCreateListener
import org.javacord.api.listener.message.MessageEditListener
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.json
import java.io.File
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

@Serializable
data class AutoModEntry(var filteredSequence: String, var punishment: Punishment?, var punishmentVal: String)

enum class Punishment {
    TIMEOUT, KICK, BAN;

    fun get(name: String) = entries.find { name.equals(it.toString(), true) }
}

object AutoMod {
    private val file = File(Main.appDir, "automod.json")
    private val excludedChannels = mutableListOf<ServerTextChannel>()
    var automods = mutableListOf<AutoModEntry>()

    fun load() {
        if (file.exists()) {
            automods = json.decodeFromString(file.readText())
        }

        Main.server.textChannels.forEach { channel ->
            if (!channel.category.isPresent)
                return@forEach

            val name = channel.category.get().name
            if (Main.config.automodExcludedCategories.find { it.equals(name, true) } != null)
                excludedChannels.add(channel)
        }
    }

    fun save() {
        file.writeText(json.encodeToString(automods))
    }

    fun automodCreateListener(): MessageCreateListener {
        return MessageCreateListener { event -> checkMessage(event.message, event.messageAuthor, event.messageContent) }
    }

    fun automodEditListener(): MessageEditListener {
        return MessageEditListener { event -> checkMessage(event.message, event.messageAuthor, event.messageContent) }
    }

    private fun checkMessage(message: Message, author: MessageAuthor, content: String) {
        var isStaff = author.isServerAdmin
        val user = author.asUser().getOrNull() ?: return
        Main.config.modRoles().forEach {
            if (isStaff)
                return@forEach
            isStaff = it.hasUser(user)
        }
        if (isStaff || excludedChannels.find { it.id == message.channel.id } != null)
            return

        for (automod in automods) {
            val pattern = Regex(automod.filteredSequence, RegexOption.IGNORE_CASE)
            if (!pattern.matches(content) && !content.contains(automod.filteredSequence, true))
                continue

            val embed = EmbedBuilder().setTitle("Automod").setAuthor(author).setUrl(message.link.toString()).setFooter("UserID: ${author.id}")
            if (automod.punishment == null) {
                embed.setDescription("Message deleted:\n```$content```")
                message.delete()
            } else {
                when (automod.punishment!!) {
                    Punishment.KICK -> {
                        Main.server.kickUser(user, "Automod")
                        message.delete()
                        embed.setDescription("Kicked & Message deleted:\n```$content```")
                    }

                    Punishment.BAN -> {
                        Main.server.banUser(author.id, Duration.ZERO, "Automod")
                        message.delete()
                        embed.setDescription("Banned & Message deleted:\n```$content```")
                    }

                    Punishment.TIMEOUT -> {
                        Main.server.timeoutUser(user, Duration.ofMinutes(automod.punishmentVal.toLong()), "Automod")
                        message.delete()
                        embed.setDescription("Timeouted & Message deleted:\n```$content```")
                    }
                }
            }
            Main.config.userLogChannel()!!.sendMessage(embed)
            break
        }
    }
}

