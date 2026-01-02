package dev.cypdashuhn.cbg.commands.nodes

import dev.cypdashuhn.cbg.canDelete
import dev.cypdashuhn.cbg.commands.args.groupNameArgument
import dev.cypdashuhn.cbg.commands.getWorldNames
import dev.cypdashuhn.cbg.commands.hasGroupsInScope
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.World

internal fun buildDeleteCommand(): Argument<String> {
    return LiteralArgument("delete")
        .withPermission(canDelete)
        .withRequirement(::hasGroupsInScope)
        .then(
            groupNameArgument()
                .executesPlayer(PlayerCommandExecutor { sender, args ->
                    val groupName: String by args.argsMap
                    val worlds = args.argsMap["worlds"] as? List<World>?
                    val worldNames = getWorldNames(worlds, sender)

                    GroupManager.deleteGroup(groupName, worldNames)
                    sender.tSend("cbg.delete.success", "groupname" to groupName)
                })
        )
}
