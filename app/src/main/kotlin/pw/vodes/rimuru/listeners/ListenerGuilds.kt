package pw.vodes.rimuru.listeners

import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pw.vodes.rimuru.services.styx.StyxProfileService

class ListenerGuilds : ListenerAdapter() {
    override fun onGuildJoin(event: GuildJoinEvent) {
        StyxProfileService.applyProfile(event.guild)
    }
}
