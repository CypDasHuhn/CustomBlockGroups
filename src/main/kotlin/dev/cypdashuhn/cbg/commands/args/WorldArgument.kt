package dev.cypdashuhn.cbg.commands.args

import dev.cypdashuhn.cbg.commands.defaultWorlds
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.TextArgument
import org.bukkit.Bukkit
import org.bukkit.World

fun worldsArgument(key: String = "worlds"): Argument<List<World>> {
    return CustomArgument(TextArgument(key)) { info ->
        val arg = info.input

        when {
            arg == "-current" -> listOf((info.sender as org.bukkit.entity.Player).world)
            arg == "-global" -> Bukkit.getWorlds()
            arg.startsWith("-") -> {
                val worldNames = arg.drop(1).split(",")
                val seenWorlds = mutableListOf<String>()
                val worlds = mutableListOf<World>()

                worldNames.forEach { worldName ->
                    val world = Bukkit.getWorld(worldName)
                    if (world == null) {
                        info.sender.tSend("cbg.error.world_not_found", "worldname" to worldName)
                        throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                            CustomArgument.MessageBuilder("World does not exist: $worldName in ").appendArgInput()
                        )
                    }

                    if (worldName in seenWorlds) {
                        info.sender.tSend("cbg.error.world_duplicate", "worldname" to worldName)
                        throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                            CustomArgument.MessageBuilder("World already specified: $worldName in ").appendArgInput()
                        )
                    }

                    seenWorlds.add(worldName)
                    worlds.add(world)
                }

                worlds
            }
            else -> defaultWorlds(info.sender)
        }
    }.replaceSuggestions(
        ArgumentSuggestions.strings { info ->
            val arg = info.currentArg
            val list = mutableListOf("-current", "-global")

            val base = arg.substringBeforeLast(",", "").trim()

            val worldNames = if (base.isEmpty() || !arg.startsWith("-")) {
                Bukkit.getWorlds().map { "-${it.name}" }
            } else {
                if (base == "-current" || base == "-global") {
                    return@strings list.toTypedArray()
                }
                val alreadyUsedWorlds = base.drop(1).split(",")

                Bukkit.getWorlds().filter { it.name !in alreadyUsedWorlds }.map { "$base,${it.name}" }
            }
            list.addAll(worldNames)

            list.toTypedArray()
        }
    )
}
