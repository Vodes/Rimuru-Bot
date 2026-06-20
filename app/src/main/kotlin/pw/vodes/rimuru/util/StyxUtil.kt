package pw.vodes.rimuru.util

import moe.styx.db.DBClient
import pw.vodes.anilistkmp.AnilistApiClient
import pw.vodes.rimuru.config.ConfigService

val dbClient by lazy {
    val config = ConfigService.getAppConfigBlocking()
    DBClient(
        "jdbc:postgresql://${config.styxConfig.dbHost}/Styx",
        "org.postgresql.Driver",
        config.styxConfig.dbUsername,
        config.styxConfig.dbPassword,
        5
    )
}

val anilistClient by lazy {
    AnilistApiClient()
}