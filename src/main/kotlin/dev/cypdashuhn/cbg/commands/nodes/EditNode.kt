package dev.cypdashuhn.cbg.commands.nodes

import dev.cypdashuhn.cbg.canEdit
import dev.cypdashuhn.cbg.commands.args.groupNameArgument
import dev.cypdashuhn.cbg.commands.args.materialListArgument
import dev.cypdashuhn.cbg.commands.args.worldsArgument
import dev.cypdashuhn.cbg.commands.getWorldNames
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.Material
import org.bukkit.World

internal fun buildEditCommand(): Argument<String> {
    return LiteralArgument("edit")
        .withPermission(canEdit)
        .then(
            groupNameArgument()
                .then(
                    materialListArgument()
                        .executesPlayer(PlayerCommandExecutor { sender, args ->
                            val groupName: String by args.argsMap
                            val materials: List<Material> by args.argsMap
                            val worlds: List<World>? by args.argsMap
                            val worldNames = getWorldNames(worlds, sender)

                            GroupManager.modifyGroup(groupName, worldNames, materials)
                            sender.tSend("cbg.edit.success", "groupname" to groupName)
                        })
                )
        )
}
