package dev.cypdashuhn.cbg.commands.nodes

import dev.cypdashuhn.cbg.canSee
import dev.cypdashuhn.cbg.commands.args.groupNameArgument
import dev.cypdashuhn.cbg.commands.args.worldsArgument
import dev.cypdashuhn.cbg.commands.getWorldNames
import dev.cypdashuhn.cbg.commands.hasGroupsInScope
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import kotlin.collections.getValue
import kotlin.collections.joinToString

internal fun buildInfoCommand(): Argument<String> {
    return LiteralArgument("info")
        .withPermission(canSee)
        .withRequirement(::hasGroupsInScope)
        .then(
            groupNameArgument()
                .executesPlayer(PlayerCommandExecutor { sender, args ->
                    val worlds = args.argsMap["worlds"] as? List<World>?
                    showGroupInfo(sender, args, worlds)
                })
        )
}

fun showGroupInfo(sender: CommandSender, args: CommandArguments, worlds: List<World>?) {
    val groupName: String by args.argsMap
    val worldNames = getWorldNames(worlds, sender)

    fun List<Material>.joinToString(): String =
        this.joinToString("<green>, <gold>") { it.name.lowercase() }

    val groups = GroupManager.getGroups(worldNames)
    val materialGroups = groups
        .filter { it.name == groupName }
        .groupBy { it.newMaterials?.joinToString() to it.oldMaterials?.joinToString() }

    lateinit var messageKey: String
    val messageReplacements: MutableList<Pair<String, String>> = mutableListOf()

    if (materialGroups.size == 1) {
        val (newMaterials, oldMaterials) = materialGroups.keys.first()
        if (newMaterials == null) {
            messageReplacements.add("materials" to oldMaterials!!)
            messageKey = "cbg.info.materials"
        } else {
            messageReplacements.add("materials" to newMaterials)
            if (oldMaterials != null) {
                messageKey = "cbg.info.materials_modified"
                messageReplacements.add("oldmaterials" to oldMaterials)
            } else {
                messageKey = "cbg.info.materials_unregistered"
            }
        }
        sender.tSend(messageKey, *messageReplacements.toTypedArray())
    } else {
        sender.tSend("cbg.info.materials_multiple", "groupname" to groupName)

        groups.forEach { group ->
            messageReplacements.clear()

            val (newMaterials, oldMaterials) = group.newMaterials?.joinToString() to group.oldMaterials?.joinToString()
            messageReplacements.add("worldname" to group.worldName)
            if (newMaterials == null) {
                messageReplacements.add("materials" to oldMaterials!!)
                messageKey = "cbg.info.materials_entry"
            } else {
                messageReplacements.add("materials" to newMaterials)
                if (oldMaterials != null) {
                    messageKey = "cbg.info.materials_entry_modified"
                    messageReplacements.add("oldmaterials" to oldMaterials)
                } else {
                    messageKey = "cbg.info.materials_entry_unregistered"
                }
            }

            sender.tSend(messageKey, *messageReplacements.toTypedArray())
        }
    }
}
