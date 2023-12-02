package pw.vodes.rimurukt

import kotlinx.coroutines.delay
import org.javacord.api.entity.message.embed.EmbedBuilder

object LogRepeater {
    var embedsToSend = mutableListOf<EmbedBuilder>()

    fun start() {
        launchGlobal {
            while (true) {
                val list = embedsToSend.toList()
                embedsToSend.clear()
                list.forEach {
                    try {
                        Main.config.userLogChannel()!!.sendMessage(it)
                    } catch (_: Exception) {
                        embedsToSend.add(it)
                    }
                }
                delay(15000)
            }
        }
    }
}