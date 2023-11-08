package pw.vodes.rimurukt.file

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.permission.Role
import org.javacord.api.entity.server.Server
import org.javacord.api.listener.message.reaction.ReactionAddListener
import org.javacord.api.listener.message.reaction.ReactionRemoveListener
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.json
import java.io.File
import kotlin.jvm.optionals.getOrNull

@Serializable
data class AutoRole(val serverID: String, val channelID: String, val messageID: String, val roleID: String) {

    @Transient
    var reactionAddListener: ReactionAddListener? = null

    @Transient
    var reactionRemoveListener: ReactionRemoveListener? = null

    private fun server(): Server? {
        return Main.api.getServerById(this.serverID).getOrNull()
    }

    private fun channel(): ServerTextChannel? {
        return server()?.getTextChannelById(channelID)?.getOrNull()
    }

    fun role(): Role? {
        return server()?.getRoleById(roleID)?.getOrNull()
    }

    fun message(): Message? {
        return channel()?.getMessageById(messageID)?.get()
    }
}


object AutoRoles {
    private val file = File(Main.appDir, "autoroles.json")
    private var autoroles = mutableListOf<AutoRole>()

    fun load() {
        if (file.exists())
            autoroles = json.decodeFromString(file.readText())

        autoroles.forEach {
            try {
                val message = it.message()!!
                message.canYouDelete()
                addListeners(it)
            } catch (_: Exception) {
            }
        }
    }

    private fun save() {
        file.writeText(json.encodeToString(autoroles))
    }

    fun addAutoRole(serverID: String, channelID: String, messageID: String, roleID: String): Boolean {
        val ar = AutoRole(serverID, channelID, messageID, roleID)
        val result = addListeners(ar)
        if (!result)
            return false

        autoroles.add(ar)
        save()
        return true
    }

    fun removeAutoRole(autoRole: AutoRole) {
        autoroles.remove(autoRole)
        save()

        val message = autoRole.message() ?: return

        if (autoRole.reactionAddListener != null)
            message.removeListener(ReactionAddListener::class.java, autoRole.reactionAddListener)

        if (autoRole.reactionRemoveListener != null)
            message.removeListener(ReactionRemoveListener::class.java, autoRole.reactionRemoveListener)
    }

    private fun addListeners(autoRole: AutoRole): Boolean {
        val role = autoRole.role() ?: return false
        try {
            val message = autoRole.message() ?: return false

            message.addReactionAddListener(ReactionAddListener { event ->
                val user = event.user.getOrNull() ?: return@ReactionAddListener
                if (role.hasUser(user))
                    role.addUser(user)
            }.also { autoRole.reactionAddListener = it })

            message.addReactionRemoveListener(ReactionRemoveListener { event ->
                val user = event.user.getOrNull() ?: return@ReactionRemoveListener
                if (role.hasUser(user))
                    role.removeUser(user)
            }.also { autoRole.reactionRemoveListener = it })

            return true
        } catch (_: Exception) {
        }
        return false
    }
}