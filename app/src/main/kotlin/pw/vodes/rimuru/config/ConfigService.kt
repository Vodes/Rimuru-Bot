package pw.vodes.rimuru.config

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object ConfigService {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val configRoot: Path = resolveConfigRoot()
    private val guildConfigRoot: Path = Path(configRoot, "guilds")
    private val appConfigFile: Path = Path(configRoot, "app-config.json")

    private val appStore: KStore<AppConfig> =
        storeOf(appConfigFile, AppConfig(), true, json)

    private val guildStores = ConcurrentHashMap<Long, KStore<GuildConfig>>()

    private fun resolveConfigRoot(): Path {
        val overrideDir = System.getenv("RIMURU2_CONFIG_DIR")?.trim().orEmpty()
        if (overrideDir.isNotBlank()) {
            return Path(overrideDir)
        }

        if (isDocker()) {
            return Path("/config")
        }

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        return if (isWindows) {
            Path(System.getenv("APPDATA"), "Vodes", "RimuruKt2")
        } else {
            val xdgConfig = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                ?: Path(System.getProperty("user.home"), ".config").toString()
            Path(xdgConfig, "Vodes", "RimuruKt2")
        }
    }

    private fun isDocker(): Boolean = SystemFileSystem.exists(Path("/.dockerenv"))

    suspend fun init() {
        SystemFileSystem.createDirectories(guildConfigRoot)
        if (!SystemFileSystem.exists(appConfigFile)) {
            appStore.set(AppConfig())
        }
    }

    fun initBlocking() = runBlocking { init() }

    suspend fun getAppConfig(): AppConfig = appStore.get() ?: AppConfig()

    fun getAppConfigBlocking(): AppConfig = runBlocking { getAppConfig() }

    suspend fun updateAppConfig(transform: (AppConfig) -> AppConfig) {
        appStore.update { current -> transform(current ?: AppConfig()) }
    }

    fun updateAppConfigBlocking(transform: (AppConfig) -> AppConfig) = runBlocking {
        updateAppConfig(transform)
    }

    private fun guildStore(guildId: Long): KStore<GuildConfig> {
        return guildStores.computeIfAbsent(guildId) { id ->
            val file = Path(guildConfigRoot, "$id.json")
            storeOf(file, GuildConfig(), true, json)
        }
    }

    suspend fun getGuildConfig(guildId: Long): GuildConfig = guildStore(guildId).get() ?: GuildConfig()

    fun getGuildConfigBlocking(guildId: Long): GuildConfig = runBlocking { getGuildConfig(guildId) }

    suspend fun updateGuildConfig(guildId: Long, transform: (GuildConfig) -> GuildConfig) {
        guildStore(guildId).update { current -> transform(current ?: GuildConfig()) }
    }

    fun updateGuildConfigBlocking(guildId: Long, transform: (GuildConfig) -> GuildConfig) = runBlocking {
        updateGuildConfig(guildId, transform)
    }

    fun isStyxEnabledForGuild(guildId: Long): Boolean {
        return getAppConfigBlocking().styxEnabledGuildId == guildId
    }

    fun shutdown() {
        appStore.close()
        guildStores.values.forEach { it.close() }
        guildStores.clear()
    }
}
