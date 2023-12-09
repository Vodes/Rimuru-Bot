package pw.vodes.rimurukt.command.commands

import org.javacord.api.entity.Icon
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.permission.PermissionType
import org.javacord.api.entity.server.Server
import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.command.Command
import pw.vodes.rimurukt.eqI
import pw.vodes.rimurukt.reportException
import kotlin.jvm.optionals.getOrNull

class CommandStealEmote : Command("Steal Emote", arrayOf("stealemote", "steal-emote", "steal")) {
    init {
        usage = """
            `!steal <emote/list of emotes>`\nWill take emotes from the message and add to this server.\n
             You may also reply to a message containing custom emotes. This will only copy the first one.
        """.trimIndent()
    }

    override fun run(event: MessageCreateEvent) {
        val server = event.server.getOrNull() ?: return
        if (!server.hasAnyPermission(event.messageAuthor.asUser().getOrNull() ?: return, PermissionType.MANAGE_EMOJIS)) {
            event.channel.sendMessage("You do not have the permission to manage emotes.")
            return
        }

        val args = args(event)
        if (args[1].isNotBlank() && event.message.customEmojis.isNotEmpty()) {
            if (event.message.customEmojis.size > 1) {
                for (emote in event.message.customEmojis) {
                    if (emoteWithNameExists(server, emote.name))
                        continue

                    createEmote(event.serverTextChannel.get(), server, emote.image, name)
                }
            } else {
                val emote = event.message.customEmojis[0]
                val name = args[2].ifBlank { emote.name }
                if (emoteWithNameExists(server, name)) {
                    event.channel.sendMessage("An emote with that name already exists! Please pass a new name.")
                    return
                }
                createEmote(event.serverTextChannel.get(), server, emote.image, name)
            }
        } else if (event.message.referencedMessage.isPresent) {
            val referenced = event.message.referencedMessage.get()
            if (referenced.customEmojis.isEmpty()) {
                event.channel.sendMessage("The referenced message does not contain any custom emotes!")
                return
            }
            val emote = referenced.customEmojis[0]
            val name = args[1].ifBlank { emote.name }
            if (emoteWithNameExists(server, name)) {
                event.channel.sendMessage("An emote with that name already exists! Please pass a new name.")
                return
            }
            createEmote(event.serverTextChannel.get(), server, emote.image, name)
        } else {
            event.channel.sendMessage(usage)
        }
    }

    private fun createEmote(channel: ServerTextChannel, server: Server, image: Icon, name: String) {
        val future = server.createCustomEmojiBuilder()
            .setImage(image)
            .setName(name)
            .create()
        future.thenAccept {
            channel.sendMessage("Added emote ${it.mentionTag}!")
        }.exceptionally {
            reportException(it, this.javaClass.canonicalName)
            channel.sendMessage("Failed to add emote!")
            null
        }
    }

    private fun emoteWithNameExists(server: Server, name: String) = server.customEmojis.find { it.name eqI name } != null
}