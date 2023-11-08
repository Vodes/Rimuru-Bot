package pw.vodes.rimurukt

import java.time.Instant

fun Instant.getRelativeTimestamp(): String {
    return "<t:${this.epochSecond}:R>"
}

fun Instant.getAbsoluteTimestamp(): String {
    return "<t:${this.epochSecond}:d> <t:${this.epochSecond}:t>"
}