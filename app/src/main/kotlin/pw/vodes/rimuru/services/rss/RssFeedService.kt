package pw.vodes.rimuru.services.rss

import pw.vodes.rimuru.config.ConfigService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object RssFeedService {
    private const val INITIAL_DELAY_SECONDS = 30L
    private const val POLL_INTERVAL_MINUTES = 10L
    private const val DELAY_BETWEEN_FEEDS_SECONDS = 40L

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rimuru-rss").apply { isDaemon = true }
    }

    private val lock = Any()
    private var feeds: List<RssFeed> = emptyList()
    private var task: ScheduledFuture<*>? = null

    fun start() {
        synchronized(lock) {
            feeds = ConfigService.getRssConfigBlocking().feeds
            if (task != null && !task!!.isCancelled) {
                return
            }

            task = scheduler.scheduleWithFixedDelay(
                { runCatching { pollFeeds() }.onFailure { reportPollLoopException(it) } },
                INITIAL_DELAY_SECONDS,
                POLL_INTERVAL_MINUTES * 60,
                TimeUnit.SECONDS
            )
        }
    }

    fun shutdown() {
        task?.cancel(false)
        scheduler.shutdownNow()
    }

    fun getFeeds(): List<RssFeed> = synchronized(lock) { feeds.toList() }

    fun replaceFeeds(updatedFeeds: List<RssFeed>) {
        synchronized(lock) {
            feeds = updatedFeeds.toList()
        }
        saveFeeds(updatedFeeds)
    }

    fun reload() {
        synchronized(lock) {
            feeds = ConfigService.getRssConfigBlocking().feeds
        }
    }

    private fun pollFeeds() {
        val currentFeeds = synchronized(lock) { feeds.toList() }
        if (currentFeeds.isEmpty()) {
            return
        }

        val u2Passkey = ConfigService.getAppConfigBlocking().u2Passkey
        val updatedFeeds = currentFeeds.toMutableList()

        updatedFeeds.indices.forEach { index ->
            val current = updatedFeeds[index]
            println("Checking RSS feed: ${current.name}")
            updatedFeeds[index] = runCatching {
                current.check(u2Passkey)
            }.getOrElse { exception ->
                reportRssException(exception, "Polling RSS feed ${current.name} failed", current.guildId)
                current
            }
            synchronized(lock) {
                feeds = updatedFeeds.toList()
            }
            saveFeeds(updatedFeeds)

            if (index != updatedFeeds.lastIndex) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(DELAY_BETWEEN_FEEDS_SECONDS))
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    private fun saveFeeds(feedsToSave: List<RssFeed>) {
        ConfigService.updateRssConfigBlocking { current ->
            current.copy(feeds = feedsToSave.toList())
        }
    }

    private fun reportPollLoopException(exception: Throwable) {
        reportRssException(exception, "RSS poll loop failed")
    }
}
