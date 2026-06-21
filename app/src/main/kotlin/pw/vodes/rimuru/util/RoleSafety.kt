package pw.vodes.rimuru.util

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role

object RoleSafety {
    private val blockedAutomaticRolePermissions = setOf(
        Permission.ADMINISTRATOR,
        Permission.MANAGE_ROLES,
        Permission.MANAGE_PERMISSIONS,
        Permission.MANAGE_CHANNEL,
        Permission.MANAGE_SERVER,
        Permission.MANAGE_WEBHOOKS,
        Permission.MANAGE_EVENTS,
        Permission.MANAGE_GUILD_EXPRESSIONS,
        Permission.KICK_MEMBERS,
        Permission.BAN_MEMBERS,
        Permission.MODERATE_MEMBERS,
        Permission.MESSAGE_MANAGE,
        Permission.MANAGE_THREADS,
        Permission.MESSAGE_MENTION_EVERYONE,
        Permission.NICKNAME_MANAGE
    )

    fun automaticRoleRejectionReason(role: Role): String? {
        if (role.isPublicRole) {
            return "@everyone cannot be assigned automatically."
        }
        if (role.isManaged) {
            return "Managed/integration roles cannot be assigned automatically."
        }
        if (!role.guild.selfMember.canInteract(role)) {
            return "I cannot assign that role because it is above or equal to my highest role."
        }

        val blockedPermissions = blockedAutomaticRolePermissions.filter { role.hasPermission(it) }
        if (blockedPermissions.isNotEmpty()) {
            return "Automatic roles cannot include privileged permissions: ${
                blockedPermissions.joinToString { it.getName() }
            }."
        }

        return null
    }

    fun canAssignAutomatically(role: Role): Boolean = automaticRoleRejectionReason(role) == null
}
