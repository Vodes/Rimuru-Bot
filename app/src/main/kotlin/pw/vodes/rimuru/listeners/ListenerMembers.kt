package pw.vodes.rimuru.listeners

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pw.vodes.rimuru.services.verification.UnverifiedPurgeService

class ListenerMembers : ListenerAdapter() {

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        UnverifiedPurgeService.onMemberJoin(event.member)
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        UnverifiedPurgeService.onMemberLeave(event.guild.idLong, event.user.idLong)
    }
}
