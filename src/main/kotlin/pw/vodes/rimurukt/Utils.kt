package pw.vodes.rimurukt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

val json = Json {
    prettyPrint = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = true
}

fun getAppDir(): File {
    return if (System.getProperty("os.name").lowercase().contains("win")) {
        val mainDir = File(System.getenv("APPDATA"), "Vodes")
        val dir = File(mainDir, "RimuruKt")
        dir.mkdirs()
        dir
    } else {
        val configDir = File(System.getProperty("user.dir"), ".config")
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