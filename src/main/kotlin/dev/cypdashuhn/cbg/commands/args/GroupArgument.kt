package dev.cypdashuhn.cbg.commands.args

import dev.cypdashuhn.cbg.commands.defaultWorlds
import dev.cypdashuhn.cbg.commands.validateFileName
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.TextArgument
import org.bukkit.World

fun groupNameArgument(
    key: String = "groupName",
    mustExist: Boolean = true,
    worldsKey: String = "worlds"
): Argument<String> {
    return CustomArgument(TextArgument(key)) { info ->
        val worlds = info.previousArgs[worldsKey] as? List<World> ?: defaultWorlds(info.sender)
        val worldNames = worlds.map { it.name }
        val groupName = info.input

        if (mustExist) {
            // Group must exist
            if (GroupManager.getGroups(worldNames).none { it.name == groupName }) {
                info.sender.tSend("cbg.error.group_not_found", "groupname" to groupName)
                throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                    CustomArgument.MessageBuilder("Group not found: ").appendArgInput()
                )
            }
        } else {
            // Group must NOT exist (for create command)
            if (GroupManager.getGroups(worldNames).any { it.name == groupName }) {
                info.sender.tSend("cbg.error.group_exists", "groupname" to groupName)
                throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                    CustomArgument.MessageBuilder("Group already exists: ").appendArgInput()
                )
            }
            // Validate filename
            validateFileName(groupName, info.sender)
        }

        groupName
    }.replaceSuggestions(
        ArgumentSuggestions.strings { info ->
            if (mustExist) {
                val worlds = info.previousArgs[worldsKey] as? List<World> ?: defaultWorlds(info.sender)
                val worldNames = worlds.map { it.name }
                GroupManager.getGroups(worldNames).map { it.name }.distinct().toTypedArray()
            } else {
                arrayOf("[Group Name]")
            }
        }
    )
}

