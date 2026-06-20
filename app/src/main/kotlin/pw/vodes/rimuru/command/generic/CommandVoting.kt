package pw.vodes.rimuru.command.generic

import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import pw.vodes.anilistkmp.ext.searchMedia
import pw.vodes.anilistkmp.graphql.fragment.MediaBig
import pw.vodes.anilistkmp.graphql.type.MediaFormat
import pw.vodes.anilistkmp.graphql.type.MediaSeason
import pw.vodes.anilistkmp.graphql.type.MediaSort
import pw.vodes.rimuru.command.Command
import pw.vodes.rimuru.command.CommandType
import pw.vodes.rimuru.config.ConfigService
import pw.vodes.rimuru.services.logging.GuildExceptionLogService
import pw.vodes.rimuru.services.styx.StyxVotingService
import pw.vodes.rimuru.util.anilistClient
import java.time.LocalDate
import kotlin.concurrent.thread

class CommandVoting : Command("voting", CommandType.ADMIN, "Create Styx anime season votings") {

    override fun guildOnly() = true

    override fun createCommand() = slashCommand()
        .addOptions(
            seasonOption(),
            OptionData(OptionType.INTEGER, "amount", "Number of shows to fetch, defaults to 25", false)
                .setRequiredRange(3, 30),
            OptionData(OptionType.INTEGER, "year", "AniList season year, defaults to the current year", false)
                .setRequiredRange(2000, 3000)
        )

    override fun run(event: SlashCommandInteractionEvent) {
        val guildContext = requireGuildContext(event, requireConfiguredAdmin = true) ?: return
        val guild = guildContext.guild
        if (!ConfigService.isStyxEnabledForGuild(guild.idLong)) {
            event.reply("This command can only be used in the configured Styx server.").setEphemeral(true).queue()
            return
        }

        val channel = guild.getChannelById(GuildMessageChannel::class.java, event.channel.idLong)
        if (channel == null) {
            event.reply("This command needs to be run in a server message channel.").setEphemeral(true).queue()
            return
        }

        val season = MediaSeason.safeValueOf(event.getOption("season")?.asString.orEmpty())
            .takeUnless { it == MediaSeason.UNKNOWN__ } ?: run {
            event.reply("Invalid AniList season.").setEphemeral(true).queue()
            return
        }
        val year = event.getOption("year")?.asLong?.toInt() ?: LocalDate.now().year
        val amount = event.getOption("amount")?.asLong?.toInt() ?: 25

        event.deferReply(true).queue()
        thread(start = true, isDaemon = true, name = "styx-voting-create") {
            val response = runCatching {
                createVotings(channel, season, year, amount)
            }.onFailure {
                GuildExceptionLogService.report(guild.idLong, "CommandVoting", it)
            }.getOrElse {
                "Failed to create votings: ${it.message ?: "unknown error"}"
            }
            event.hook.editOriginal(response).queue()
        }
    }

    private fun createVotings(
        channel: GuildMessageChannel,
        season: MediaSeason,
        year: Int,
        amount: Int
    ): String {
        val shows = runBlocking {
            anilistClient.searchMedia(
                sort = listOf(MediaSort.POPULARITY_DESC),
                season = season,
                seasonYear = year,
                formatIn = listOf(MediaFormat.TV, MediaFormat.ONA, MediaFormat.OVA),
                page = 1,
                perPage = 50
            )
        }

        shows.exception?.let { throw it }
        val anilistErrors = shows.errors.orEmpty()
        if (anilistErrors.isNotEmpty()) {
            return "AniList returned an error: ${anilistErrors.joinToString { it.message }}"
        }

        val failed = mutableListOf<Int>()
        val created = shows.data.take(amount).count { media ->
            runCatching {
                val message = channel.sendMessageEmbeds(buildEmbed(media).build()).complete()
                StyxVotingService.createInitialVoting(media.displayTitle(), media.id, message)
                message.addReaction(StyxVotingService.voteEmoji(channel.guild)).complete()
            }.onFailure {
                failed.add(media.id)
                it.printStackTrace()
            }.isSuccess
        }

        return when {
            created == 0 && failed.isEmpty() -> "No eligible shows found for ${season.displayName()} $year."
            failed.isEmpty() -> "Created $created voting(s)."
            else -> "Created $created voting(s). Failed AniList IDs: ${failed.joinToString()}."
        }
    }

    private fun buildEmbed(media: MediaBig): EmbedBuilder {
        val embed = EmbedBuilder()
            .setTitle(media.displayTitle(), "https://anilist.co/anime/${media.id}")

        media.title?.romaji
            ?.takeIf { it.isNotBlank() && !it.equals(media.displayTitle(), ignoreCase = true) }
            ?.let { embed.setDescription(it) }

        media.startDate?.formatDate()?.let { embed.addField("Start Date", it, true) }
        media.format?.rawValue?.let { embed.addField("Format", it, true) }
        media.studios?.nodes?.let { nodes -> embed.addField("Studios", nodes.filterNotNull().joinToString { it.name }, true) }

        media.genres?.filterNotNull()?.takeIf { it.isNotEmpty() }?.let {
            embed.addField("Genres", it.joinToString(), false)
        }

        val trailer = media.trailer?.takeIf { it.site.equals("youtube", ignoreCase = true) && !it.id.isNullOrBlank() }
            ?.let { "[Trailer](https://youtu.be/${it.id}) " }
            .orEmpty()
        embed.addField("Links", "${trailer}[AniList](https://anilist.co/anime/${media.id})", false)

        media.coverImage?.extraLarge?.let { embed.setThumbnail(it) }
            ?: media.coverImage?.large?.let { embed.setThumbnail(it) }

        return StyxVotingService.setVotingAuthor(embed)
    }

    private fun seasonOption(): OptionData {
        return OptionData(OptionType.STRING, "season", "AniList season to fetch", true)
            .addChoices(
                Choice("Winter", MediaSeason.WINTER.rawValue),
                Choice("Spring", MediaSeason.SPRING.rawValue),
                Choice("Summer", MediaSeason.SUMMER.rawValue),
                Choice("Fall", MediaSeason.FALL.rawValue)
            )
    }

    private fun MediaSeason.displayName(): String {
        return rawValue.lowercase().replaceFirstChar { it.titlecase() }
    }

    private fun MediaBig.displayTitle(): String {
        return title?.english?.takeIf { it.isNotBlank() }
            ?: title?.romaji?.takeIf { it.isNotBlank() }
            ?: title?.native?.takeIf { it.isNotBlank() }
            ?: "AniList #$id"
    }

    private fun MediaBig.StartDate.formatDate(): String? {
        val year = year ?: return null
        val month = month?.toString()?.padStart(2, '0') ?: "??"
        val day = day?.toString()?.padStart(2, '0') ?: "??"
        return "$day.$month.$year"
    }
}
