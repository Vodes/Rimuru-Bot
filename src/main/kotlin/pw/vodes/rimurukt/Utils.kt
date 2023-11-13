package pw.vodes.rimurukt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.peanuuutz.tomlkt.Toml
import org.javacord.api.entity.message.embed.EmbedBuilder
import java.io.File
import java.security.SecureRandom
import java.time.Instant
import kotlin.math.min

val json = Json {
    prettyPrint = true
    isLenient = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

val toml = Toml {
    ignoreUnknownKeys = true
    explicitNulls = true
}

val random = SecureRandom()

fun epochSecond() = Instant.now().epochSecond

fun getAppDir(): File {
    return if (System.getProperty("os.name").lowercase().contains("win")) {
        val mainDir = File(System.getenv("APPDATA"), "Vodes")
        val dir = File(mainDir, "RimuruKt")
        dir.mkdirs()
        dir
    } else {
        val configDir = File(System.getProperty("user.home"), ".config")
        val mainDir = File(configDir, "Vodes")
        val dir = File(mainDir, "RimuruKt")
        dir.mkdirs()
        dir
    }
}

fun launchThreaded(run: suspend CoroutineScope.() -> Unit): Pair<Job, CoroutineScope> {
    val job = Job()
    val scope = CoroutineScope(job)
    scope.launch {
        run()
    }
    return Pair(job, scope)
}

fun reportException(ex: Throwable, source: String? = null) {
    var stacktrace = ex.stackTraceToString()
    var message = ex.localizedMessage
    try {
        println(stacktrace)

        val trimmed = stacktrace.subSequence(stacktrace.indexOf("\n") + 1, min(stacktrace.length, 1200))
        val description = if (trimmed.contains("pw.vodes.rimurukt")) "${message}\n```\n$trimmed```" else message
        val embed = EmbedBuilder().setTitle("Exception thrown!")
            .setDescription(description)
        if (!source.isNullOrBlank())
            embed.setFooter("Source: $source")
        Main.config.otherLogChannel()?.sendMessage(embed)
    } catch (_: Exception) {
        // Shouldn't run into this
    }
}