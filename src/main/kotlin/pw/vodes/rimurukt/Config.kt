package pw.vodes.rimurukt

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.javacord.api.entity.channel.ServerTextChannel
import org.javacord.api.entity.message.Message
import org.javacord.api.entity.permission.Role
import kotlin.jvm.optionals.getOrNull

@Serializable
data class Config(
    val botToken: String = "", val commandPrefix: String = "!",
    val generalChat: String = "",
    val userLogChannel: String = "", val otherLogChannel: String = "",

    val verificationRole: String = "",
    val verificationChannel: String = "",
    val verificationReactionMessage: String = "",
    val verifyHallOfShame: String = "",

    val u2Passkey: String = "",
    val purge3Days: Boolean = true,
    val modRoles: List<String> = listOf("Role ID 1", "Role ID 2"),
    val automodExcludedCategories: List<String> = listOf("unhinged"),

    val updateConfig: UpdateConfig = UpdateConfig()
) {
    @Transient
    private val _modRoles = mutableListOf<Role>()


    fun modRoles(): List<Role> {
        if (_modRoles.isNotEmpty())
            return _modRoles.toList()

        modRoles.forEach {
            Main.server.getRoleById(it).ifPresent { role ->
                _modRoles.add(role)
            }
        }
        return _modRoles.toList()
    }

    fun generalChat(): ServerTextChannel? {
        return if (generalChat.isBlank())
            null
        else
            Main.server.getTextChannelById(generalChat).getOrNull()
    }

    fun userLogChannel(): ServerTextChannel? {
        return if (userLogChannel.isBlank())
            null
        else
            Main.server.getTextChannelById(userLogChannel).getOrNull()
    }

    fun otherLogChannel(): ServerTextChannel? {
        return if (otherLogChannel.isBlank())
            null
        else
            Main.server.getTextChannelById(otherLogChannel).getOrNull()
    }

    fun verificationChannel(): ServerTextChannel? {
        return if (verificationChannel.isBlank())
            null
        else
            Main.server.getTextChannelById(verificationChannel).getOrNull()
    }

    fun hallOfShameChannel(): ServerTextChannel? {
        return if (verifyHallOfShame.isBlank())
            null
        else
            Main.server.getTextChannelById(verifyHallOfShame).getOrNull()
    }

    fun verificationMessage(): Message? {
        return if (verificationReactionMessage.isBlank())
            null
        else
            verificationChannel()!!.getMessageById(verificationReactionMessage).get()
    }
}

@Serializable
data class UpdateConfig(
    val gitRepo: String = "https://github.com/Vodes/Rimuru-Bot.git",
    val branch: String = "master",
    val oauthToken: String = "",
    var currentCommit: String = "",

    val customJvmArgs: String = "-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xms1G -Xmx2G",
    val allowUpdate: Boolean = true,
    var restartTrigger: String = ""
)