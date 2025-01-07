package pw.vodes.rimurukt.services.rss

import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.json
import pw.vodes.rimurukt.launchGlobal
import pw.vodes.rimurukt.reportException
import java.io.File

object RSSFeeds {

    private val file = File(Main.appDir, "rssfeeds.json")
    var feeds = mutableListOf<Feed>()

    fun load() {
        runCatching {
            if (file.exists())
                feeds = json.decodeFromString(file.readText())
            start()
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun save() {
        runCatching {
            file.writeText(json.encodeToString(feeds))
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun start() {
        launchGlobal {
            delay(30000)
            while (true) {
                feeds.forEach {
                    println("Checking: ${it.name}")
                    try {
                        it.check()
                    } catch (ex: Exception) {
                        reportException(ex, "Failed to check feed in main loop: ${it.name}")
                    }
                    delay(10000)
                    save()
                    delay(30000)
                }
                delay(600000)
            }
        }
    }

}