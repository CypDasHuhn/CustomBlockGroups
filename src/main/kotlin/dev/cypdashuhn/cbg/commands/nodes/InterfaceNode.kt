package dev.cypdashuhn.cbg.commands.nodes

import dev.cypdashuhn.cbg.canSee
import dev.cypdashuhn.cbg.commands.args.worldsArgument
import dev.cypdashuhn.cbg.commands.getWorldNames
import dev.cypdashuhn.cbg.ui.SelectGroupInterface
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.World

internal fun buildInterfaceCommand(): Argument<String> {
    return LiteralArgument("interface")
        .withPermission(canSee)
        .executesPlayer(PlayerCommandExecutor { sender, args ->
            val worlds: List<World>? by args.argsMap
            val worldNames = getWorldNames(worlds, sender)

            SelectGroupInterface.openInventory(
                sender,
                SelectGroupInterface.getContext(sender).also {
                    it.worldNames = worldNames
                }
            )
        })
}
