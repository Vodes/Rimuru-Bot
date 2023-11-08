package pw.vodes.rimurukt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.javacord.api.entity.message.embed.EmbedBuilder

object LogRepeater {
    private var job = Job()
    var embedsToSend = mutableListOf<EmbedBuilder>()

    fun start() {
        val scope = CoroutineScope(job)
        scope.launch {
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