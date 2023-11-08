package pw.vodes.rimurukt.audit

import kotlinx.serialization.Serializable
import org.javacord.api.entity.message.embed.EmbedBuilder
import pw.vodes.rimurukt.Main
import kotlin.math.abs

@Serializable
data class StaffAction(val affectedUser: String, val executingUser: String, val time: Long, val type: StaffActionType, val reason: String = "") {

    override fun equals(other: Any?): Boolean {
        if (other !is StaffAction)
            return super.equals(other)

        val diff = abs(time - other.time)

        return if (other.executingUser.equals(Main.api.yourself.idAsString, true)) {
            affectedUser.equals(other.affectedUser, true) &&
                    diff < 4 &&
                    type == other.type
        } else
            affectedUser.equals(other.affectedUser, true) &&
                    executingUser.equals(other.executingUser, true) &&
                    diff < 4 &&
                    type == other.type
    }

    fun getEmbed(): EmbedBuilder {
        val u1 = Main.api.getUserById(executingUser).get()
        val u2 = Main.api.getUserById(affectedUser).get()

        return EmbedBuilder().setTitle(type.messageTitle()).setFooter("ID: $affectedUser").setAuthor(u1).setThumbnail(u2.getAvatar(4096))
            .setDescription(u2.name + if (reason.isBlank()) "" else "\nReason:\n```$reason```")
    }
}
