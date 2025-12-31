package dev.cypdashuhn.cbg.commands.nodes

import dev.cypdashuhn.cbg.canCreate
import dev.cypdashuhn.cbg.commands.args.groupNameArgument
import dev.cypdashuhn.cbg.commands.args.materialListArgument
import dev.cypdashuhn.cbg.commands.getWorldNames
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.Material
import org.bukkit.World

internal fun buildCreateCommand(): Argument<String> {
    return LiteralArgument("create")
        .withPermission(canCreate)
        .then(
            groupNameArgument(mustExist = false)
                .then(
                    materialListArgument()
                        .executesPlayer(PlayerCommandExecutor { sender, args ->
                            val group: String by args.argsMap
                            val materials: List<Material> by args.argsMap
                            val worlds = args.argsMap.get("worlds") as? List<World>?
                            val worldNames = getWorldNames(worlds, sender)

                            GroupManager.addGroup(group, worldNames, materials)
                            sender.tSend("cbg.create.success", "groupname" to group)
                        })
                )
        )
}
