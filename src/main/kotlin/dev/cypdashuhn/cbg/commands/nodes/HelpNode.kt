package dev.cypdashuhn.cbg.commands.nodes

import dev.cypdashuhn.cbg.canSeeAdminHelp
import dev.cypdashuhn.cbg.datapackName
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor

internal fun buildHelpCommand(): Argument<String> {
    return LiteralArgument("help")
        .executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.tSend("cbg.help.general", "datapackname" to datapackName)
        })
        .then(
            LiteralArgument("general")
                .executesPlayer(PlayerCommandExecutor { sender, _ ->
                    sender.tSend("cbg.help.general", "datapackname" to datapackName)
                })
        )
        .then(
            LiteralArgument("interface")
                .executesPlayer(PlayerCommandExecutor { sender, _ ->
                    sender.tSend("cbg.help.interface")
                })
        )
        .then(
            LiteralArgument("world")
                .executesPlayer(PlayerCommandExecutor { sender, _ ->
                    sender.tSend("cbg.help.world")
                })
        )
        .then(
            LiteralArgument("admin")
                .withPermission(canSeeAdminHelp)
                .executesPlayer(PlayerCommandExecutor { sender, _ ->
                    sender.tSend("cbg.help.admin")
                })
        )
}
