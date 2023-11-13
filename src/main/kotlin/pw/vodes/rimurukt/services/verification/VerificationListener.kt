package pw.vodes.rimurukt.services.verification

import kotlinx.coroutines.delay
import org.javacord.api.entity.channel.ChannelType
import org.javacord.api.event.message.reaction.ReactionAddEvent
import org.javacord.api.listener.message.reaction.ReactionAddListener
import pw.vodes.rimurukt.Main
import pw.vodes.rimurukt.launchThreaded
import pw.vodes.rimurukt.random
import pw.vodes.rimurukt.reportException
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt

class VerificationListener : ReactionAddListener {
    override fun onReactionAdd(event: ReactionAddEvent) {
        val role = Main.server.getRoleById(Main.config.verificationRole).getOrNull() ?: return
        val user = event.user.getOrNull() ?: return

        if (role.hasUser(user))
            return

        val math = VerificationMath()
        val thread = event.channel.asServerTextChannel().get().createThread(ChannelType.SERVER_PUBLIC_THREAD, user.name, 60, true).get()

        thread.sendMessage("${user.mentionTag} ${math.message}").get()

        thread.addMessageCreateListener {
            if (it.messageAuthor.id == user.id) {
                try {
                    var answer: Double
                    try {
                        answer = it.messageContent.toDouble()
                    } catch (_: Exception) {
                        Main.server.timeoutUser(user, Duration.ofMinutes(5), "Incorrect Verification")
                        return@addMessageCreateListener
                    }
                    if (answer.roundToInt() == math.result) {
                        role.addUser(user)
                    } else {
                        Main.server.timeoutUser(user, Duration.ofMinutes(5), "Incorrect Verification")
                    }
                    it.message.delete()
                    thread.delete()
                } catch (ex: Exception) {
                    reportException(ex, this.javaClass.canonicalName)
                }
            }
        }.removeAfter(17, TimeUnit.SECONDS)

        launchThreaded {
            delay(17000)
            val thr = Main.server.getThreadChannelById(thread.id).getOrNull() ?: return@launchThreaded
            thr.delete()
        }
    }
}

private class VerificationMath {
    var message: String
    var result: Int

    init {
        var num1 = random.nextInt(80) + 10
        var num2 = random.nextInt(8) + 1
        var num3 = random.nextInt(20) + 1

        while (doubleVal(num1, num2, num3) != doubleVal(num1, num2, num3).roundToInt().toDouble()) {
            num1 = random.nextInt(80) + 10
            num2 = random.nextInt(8) + 1
            num3 = random.nextInt(20) + 1
        }

        message = "What is $num1 / $num2 - $num3"
        result = doubleVal(num1, num2, num3).roundToInt()
    }

    private fun doubleVal(num1: Int, num2: Int, num3: Int) = (num1.toDouble() / num2.toDouble()) - num3.toDouble()
}