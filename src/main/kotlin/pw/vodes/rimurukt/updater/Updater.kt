package pw.vodes.rimurukt.updater

import kotlinx.serialization.encodeToString
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.javacord.api.event.message.MessageCreateEvent
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.reportException
import pw.vodes.rimurukt.toml
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.jvm.optionals.getOrNull
import kotlin.system.exitProcess

object Updater {
    private lateinit var jarFile: File

    private fun isWin(): Boolean {
        return System.getProperty("os.name").startsWith("win", true)
    }

    fun init() {
        val path = this.javaClass.protectionDomain.codeSource.location.path
        jarFile = try {
            File(URLDecoder.decode(path, "UTF-8"))
        } catch (ex: UnsupportedEncodingException) {
            File(path)
        }
    }

    fun buildGit(event: MessageCreateEvent): Boolean {
        val repoDir = File(Main.appDir, "Rimuru-Git")
        val isWin = isWin()
        var git: Git? = null
        try {
            var msg = event.channel.sendMessage("Cloning repo...").get()
            var gitRequest = Git.cloneRepository().setURI(Main.config.updateConfig.gitRepo).setBranch(Main.config.updateConfig.branch).setDirectory(repoDir)
            if (Main.config.updateConfig.oauthToken.isNotBlank())
                gitRequest = gitRequest.setCredentialsProvider(UsernamePasswordCredentialsProvider("token", Main.config.updateConfig.oauthToken))
            git = gitRequest.call()

            val latestCommit = git.log().call().first()
            msg.edit("Repo cloned. Latest commit: `${latestCommit.name}`")

            val progress = "Building jar from latest commit..."
            msg = event.channel.sendMessage(progress).get()

            if (!isWin)
                File(repoDir, "gradlew").setExecutable(true, false)
            val process = if (isWin)
                ProcessBuilder(listOf("cmd", "/c", "gradlew.bat clean shadowJar")).directory(repoDir).start()
            else
                ProcessBuilder(listOf("sh", "-c", "./gradlew clean shadowJar")).directory(repoDir).start()

            try {
                val regex = "(\\d+)% EXECUTING".toRegex()
                val buf = BufferedReader(InputStreamReader(process.inputStream))
                var line = ""
                while (buf.readLine()?.also { line = it } != null) {
                    val match = regex.find(line)
                    if (match != null)
                        msg.edit("$progress ${match.groups[1] ?: ""}".trim())
                }
                buf.close()
            } catch (ex: Exception) {
                reportException(ex, "${this.javaClass.canonicalName} at Build Process")
            }
            val buildDir = File(File(repoDir, "build"), "libs")
            if (!buildDir.exists())
                return false

            var builtJar: File? = null
            buildDir.walk().forEach {
                if (it.extension.equals("jar", true)) {
                    builtJar = it
                }
            }
            if (builtJar == null)
                return false
            if (jarFile.extension.equals("jar", true)) {
                Files.move(builtJar!!.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } else if (jarFile.isDirectory) {
                jarFile = File(jarFile, builtJar!!.name)
                Files.move(builtJar!!.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            event.channel.sendMessage("Moved finished build. Restarting...")

            Main.config.updateConfig.currentCommit = latestCommit.name
            Main.config.updateConfig.restartTrigger = event.messageLink.toString()
            Main.configFile.writeText(toml.encodeToString(Main.config))
            return true
        } catch (ex: Exception) {
            reportException(ex, this.javaClass.canonicalName)
        } finally {
            git?.close()
            repoDir.deleteRecursively()
        }
        return false
    }

    fun restart(): Boolean {
        val isWin = isWin()
        var command = "screen -dmS Rimuru bash -c 'sleep 2; java ${Main.config.updateConfig.customJvmArgs} -jar \"${jarFile.absolutePath}\"'"
        if (isWin)
            command = "java ${Main.config.updateConfig.customJvmArgs} -jar \"${jarFile.absolutePath}\""
        try {
            // This can't run in the background on windows because windows things, but I guess it works
            if (isWin)
                Runtime.getRuntime().exec("cmd /c start java ${Main.config.updateConfig.customJvmArgs} -jar \"${jarFile.absolutePath}\"")
            else
                ProcessBuilder(listOf("bash", "-c", command)).start()
            exitProcess(0)
        } catch (ex: Exception) {
            reportException(ex, "${this.javaClass.canonicalName} at restart")
        }
        return false
    }

    fun postRestart() {
        if (Main.config.updateConfig.restartTrigger.isBlank())
            return

        val msgFuture = Main.api.getMessageByLink(Main.config.updateConfig.restartTrigger).getOrNull() ?: return
        val msg = msgFuture.join()
        msg.reply("Restarted!")
        Main.config.updateConfig.restartTrigger = ""
        Main.configFile.writeText(toml.encodeToString(Main.config))
    }
}