package pw.vodes.rimurukt.updater

import org.eclipse.jgit.api.Git
import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.reportException
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

object Updater {
    private lateinit var jarFile: File

    fun init() {
        val path = this.javaClass.protectionDomain.codeSource.location.path
        jarFile = try {
            File(URLDecoder.decode(path, "UTF-8"))
        } catch (ex: UnsupportedEncodingException) {
            File(path)
        }
    }

    fun update(event: MessageCreateEvent) {
        val repoDir = File(Main.appDir, "Rimuru-Git")
        var git: Git? = null
        try {
            var msg = event.channel.sendMessage("Cloning repo...").get()
            git = Git.cloneRepository().setURI(Main.config.updateConfig.gitRepo).setBranch(Main.config.updateConfig.branch).setDirectory(repoDir).call()

            val latestCommit = git.log().call().first()
            msg.edit("Repo cloned. Latest commit: `${latestCommit.name}`")

            var progress = "Building jar from latest commit..."
            msg = event.channel.sendMessage(progress).get()

            var process = if (System.getProperty("os.name").startsWith("win", true))
                ProcessBuilder(listOf("cmd", "/c", "")).directory(repoDir).start()
            else
                ProcessBuilder(listOf("sh", "-c", "")).directory(repoDir).start()
        } catch (ex: Exception) {
            reportException(ex, this.javaClass.canonicalName)
        } finally {
            git?.close()
            repoDir.deleteRecursively()
        }
    }
}