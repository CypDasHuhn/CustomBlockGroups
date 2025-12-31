package dev.cypdashuhn.cbg.commands

import dev.cypdashuhn.cbg.commands.args.worldsArgument
import dev.cypdashuhn.cbg.commands.nodes.*
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.executors.PlayerCommandExecutor

fun registerCbgCommand() {
    val commandNodes = listOf(
        buildHelpCommand(),
        buildCreateCommand(),
        buildInterfaceCommand(),
        buildInfoCommand(),
        buildEditCommand(),
        buildDeleteCommand()
    )

    CommandTree("cbg")
        .executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.tSend("cbg.error.missing_arg")
        })
        .apply { commandNodes.forEach { then(it) } }
        .then(
            worldsArgument().apply {
                commandNodes.forEach { then(it) }
            }
        )
        .register()
}
