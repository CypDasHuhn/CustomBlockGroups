package dev.cypdashuhn.cbg.commands

import dev.cypdashuhn.cbg.*
import dev.cypdashuhn.rooster.localization.tSend
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.cbg.ui.SelectGroupInterface
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.*
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

fun defaultWorlds(sender: CommandSender): List<World> {
    return if (globalAsDefault) Bukkit.getWorlds()
    else listOf((sender as Player).world)
}

fun getWorldNames(worlds: List<World>?, sender: CommandSender): List<String> {
    return (worlds ?: defaultWorlds(sender)).map { it.name }
}

fun groupNameArgument(
    key: String = "group",
    mustExist: Boolean = true,
    worldsKey: String = "worlds"
): Argument<String> {
    return CustomArgument(TextArgument(key)) { info ->
        val worlds = info.previousArgs["worlds"] as? List<World> ?: defaultWorlds(info.sender)
        val worldNames = worlds.map { it.name }
        val groupName = info.input

        if (mustExist) {
            // Group must exist
            if (GroupManager.getGroupsAllMatching(worldNames).none { it.name == groupName }) {
                info.sender.tSend("group_not_found", "groupName" to groupName)
                throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                    CustomArgument.MessageBuilder("Group not found: ").appendArgInput()
                )
            }
        } else {
            // Group must NOT exist (for create command)
            if (GroupManager.getGroups(worldNames).any { it.name == groupName }) {
                info.sender.tSend("group_exists", "groupName" to groupName)
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
                val worlds = info.previousArgs["worlds"] as? List<World> ?: defaultWorlds(info.sender)
                val worldNames = worlds.map { it.name }
                GroupManager.getGroupsAllMatching(worldNames).map { it.name }.toTypedArray()
            } else {
                arrayOf("[Group Name]")
            }
        }
    )
}

fun worldsArgument(key: String = "worlds"): Argument<List<World>> {
    return CustomArgument(GreedyStringArgument(key)) { info ->
        val arg = info.input

        when {
            arg == "-current" -> listOf((info.sender as Player).world)
            arg == "-global" -> Bukkit.getWorlds()
            arg.startsWith("-") -> {
                val worldNames = arg.drop(1).split(",")
                val seenWorlds = mutableListOf<String>()
                val worlds = mutableListOf<World>()

                worldNames.forEach { worldName ->
                    val world = Bukkit.getWorld(worldName)
                    if (world == null) {
                        info.sender.tSend("world_does_not_exist", "worldName" to worldName)
                        throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                            CustomArgument.MessageBuilder("World does not exist: $worldName in ").appendArgInput()
                        )
                    }

                    if (worldName in seenWorlds) {
                        info.sender.tSend("world_already_used", "worldName" to worldName)
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
    ).setOptional(true)
}

fun registerCbgCommand() {
    CommandTree("cbg")
        .executesPlayer(PlayerCommandExecutor { sender, _ ->
            sender.tSend("cbt_missing_arg")
        })
        .then(
            // Help command
            LiteralArgument("help")
                .executesPlayer(PlayerCommandExecutor { sender, _ ->
                    sender.tSend("general_help_message")
                })
                .then(
                    LiteralArgument("general")
                        .executesPlayer(PlayerCommandExecutor { sender, _ ->
                            sender.tSend("general_help_message", "datapackName" to datapackName)
                        })
                )
                .then(
                    LiteralArgument("interface")
                        .executesPlayer(PlayerCommandExecutor { sender, _ ->
                            sender.tSend("interface_help_message")
                        })
                )
                .then(
                    LiteralArgument("world")
                        .executesPlayer(PlayerCommandExecutor { sender, _ ->
                            sender.tSend("world_help_message")
                        })
                )
                .then(
                    LiteralArgument("admin")
                        .withPermission(canSeeAdminHelp)
                        .executesPlayer(PlayerCommandExecutor { sender, _ ->
                            sender.tSend("admin_help_message")
                        })
                )
        )
        .then(
            // Create command
            LiteralArgument("create")
                .withPermission(canCreate)
                .then(
                    worldsArgument()
                        .then(
                            groupNameArgument(mustExist = false)
                                .then(
                                    materialListArgument()
                                        .executesPlayer(PlayerCommandExecutor { sender, args ->
                                            val groupName: String by args.argsMap
                                            val materials: List<Material> by args.argsMap
                                            val worlds: List<World>? by args.argsMap

                                            val worldNames = getWorldNames(worlds, sender)

                                            GroupManager.addGroup(groupName, worldNames, materials)
                                            sender.tSend("created_group", "groupName" to groupName)
                                        })
                                )
                        )
                )
        )
        .then(
            // Interface command
            LiteralArgument("interface")
                .withPermission(canSee)
                .then(
                    worldsArgument()
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
                )
        )
        .then(
            // Info command
            LiteralArgument("info")
                .withPermission(canSee)
                .then(
                    worldsArgument()
                        .then(
                            groupNameArgument()
                                .executesPlayer(PlayerCommandExecutor { sender, args ->
                                    val groupName: String by args.argsMap
                                    val worlds: List<World>? by args.argsMap
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
                                            messageKey = "group_list"
                                        } else {
                                            messageReplacements.add("materials" to newMaterials)
                                            if (oldMaterials != null) {
                                                messageKey = "group_list_modified"
                                                messageReplacements.add("oldMaterials" to oldMaterials)
                                            } else {
                                                messageKey = "group_list_unregistered"
                                            }
                                        }
                                        sender.tSend(messageKey, *messageReplacements.toTypedArray())
                                    } else {
                                        sender.tSend("group_list_multiple", "groupName" to groupName)

                                        groups.forEach { group ->
                                            messageReplacements.clear()

                                            val (newMaterials, oldMaterials) = group.newMaterials?.joinToString() to group.oldMaterials?.joinToString()
                                            messageReplacements.add("worldName" to group.worldName)
                                            if (newMaterials == null) {
                                                messageReplacements.add("materials" to oldMaterials!!)
                                                messageKey = "group_list_entry"
                                            } else {
                                                messageReplacements.add("materials" to newMaterials)
                                                if (oldMaterials != null) {
                                                    messageKey = "group_list_entry_modified"
                                                    messageReplacements.add("oldMaterials" to oldMaterials)
                                                } else {
                                                    messageKey = "group_list_entry_unregistered"
                                                }
                                            }

                                            sender.tSend(messageKey, *messageReplacements.toTypedArray())
                                        }
                                    }
                                })
                        )
                )
        )
        .then(
            // Edit command
            LiteralArgument("edit")
                .withPermission(canEdit)
                .then(
                    worldsArgument()
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
                                            sender.tSend("edited_group", "groupName" to groupName)
                                        })
                                )
                        )
                )
        )
        .then(
            // Delete command
            LiteralArgument("delete")
                .withPermission(canDelete)
                .then(
                    worldsArgument()
                        .then(
                            groupNameArgument()
                                .executesPlayer(PlayerCommandExecutor { sender, args ->
                                    val groupName: String by args.argsMap
                                    val worlds: List<World>? by args.argsMap
                                    val worldNames = getWorldNames(worlds, sender)

                                    GroupManager.deleteGroup(groupName, worldNames)
                                    sender.tSend("deleted_group", "groupName" to groupName)
                                })
                        )
                )
        )
        .register()
}
