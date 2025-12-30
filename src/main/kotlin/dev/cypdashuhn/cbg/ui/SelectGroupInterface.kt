package dev.cypdashuhn.cbg.ui

import dev.cypdashuhn.cbg.commands.defaultWorlds
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.cbg.util.pagerItem
import dev.cypdashuhn.rooster.common.util.createItem
import dev.cypdashuhn.rooster.localization.t
import dev.cypdashuhn.cbg.util.minimessage
import dev.cypdashuhn.rooster.ui.interfaces.ClickInfo
import dev.cypdashuhn.rooster.ui.interfaces.InterfaceInfo
import dev.cypdashuhn.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.cypdashuhn.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import dev.cypdashuhn.rooster.ui.interfaces.options
import dev.cypdashuhn.rooster.ui.items.InterfaceItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KClass

object SelectGroupInterface :
    ScrollInterface<SelectGroupInterface.SelectGroupContext, List<SelectGroupInterface.GroupDTO>>(
        "SelectGroupInterface",
        options {
            modifyScroller = { displayAs(pagerItem()) }
        }
    ) {
    class SelectGroupContext(
        var worldNames: List<String>,
        var groupFilter: GroupFilter,
        var onlyOverlappingWorlds: Boolean
    ) : ScrollContext()

    enum class GroupFilter {
        ALL,
        IF_DIFFERENT
    }

    class GroupDTO(
        val name: String,
        val worldName: String,
        val oldMaterials: List<Material>?,
        var newMaterials: List<Material>?
    )

    override fun contentDisplay(
        data: List<GroupDTO>,
        context: SelectGroupContext
    ): InterfaceInfo<SelectGroupContext>.() -> ItemStack = {
        val description = data.map { group ->
            minimessage("<light_purple>${group.worldName}")
        }
        val first = data.first()

        val materials = first.newMaterials ?: first.oldMaterials!!
        val material =
            if (materials.first() != Material.AIR) materials.first() else materials.getOrNull(1) ?: Material.BARRIER

        createItem(
            material = material,
            name = minimessage("<yellow>${first.name}"),
            description = description
        )
    }

    override fun contentClick(
        data: List<GroupDTO>,
        context: SelectGroupContext
    ): ClickInfo<SelectGroupContext>.() -> Unit = {
        GroupInterface.openInventory(click.player, GroupInterface.GroupContext(data))
    }

    override fun contentProvider(id: Int, context: SelectGroupContext): List<GroupDTO>? {
        val result = transaction {
            val validGroupNames = GroupManager.Groups
                .select(GroupManager.Groups.name, GroupManager.Groups.worldName)
                .where { GroupManager.Groups.worldName inList context.worldNames }
                .groupBy(GroupManager.Groups.name)
                .having {
                    GroupManager.Groups.worldName.count() greaterEq context.worldNames.size.toLong()
                }.map { it[GroupManager.Groups.name] }

            val base = if (context.onlyOverlappingWorlds) {
                GroupManager.Groups
                    .selectAll()
                    .where {
                        (GroupManager.Groups.name inList validGroupNames) and
                                (GroupManager.Groups.worldName inList context.worldNames)
                    }
            } else {
                GroupManager.Groups.selectAll().where { GroupManager.Groups.worldName inList context.worldNames }
            }

            when (context.groupFilter) {
                GroupFilter.ALL -> listOf(
                    GroupManager.Group.wrapRow(
                        base.orderBy(GroupManager.Groups.name to SortOrder.DESC).limit(1, id.toLong()).firstOrNull()
                            ?: return@transaction null
                    ).toDTO()
                )

                GroupFilter.IF_DIFFERENT -> {
                    val groups = base
                        .orderBy(GroupManager.Groups.name to SortOrder.DESC)
                        .groupBy(GroupManager.Groups.newMaterials, GroupManager.Groups.oldMaterials)

                    val entry = groups.limit(1, id.toLong()).firstOrNull() ?: return@transaction null

                    var query = (GroupManager.Groups.worldName inList context.worldNames) and
                            (GroupManager.Groups.newMaterials eq entry[GroupManager.Groups.newMaterials]) and
                            (GroupManager.Groups.oldMaterials eq entry[GroupManager.Groups.oldMaterials])

                    if (context.onlyOverlappingWorlds) query =
                        query and (GroupManager.Groups.name inList validGroupNames)

                    val sameGroupEntries = GroupManager.Groups.selectAll()
                        .where {
                            query
                        }
                        .orderBy(GroupManager.Groups.name to SortOrder.DESC)
                        .map { GroupManager.Group.wrapRow(it).toDTO() }

                    return@transaction sameGroupEntries
                }
            }
        }

        return result
    }

    override val contextClass: KClass<SelectGroupContext> get() = SelectGroupContext::class

    override fun defaultContext(player: Player): SelectGroupContext {
        return SelectGroupContext(
            worldNames = defaultWorlds(player).map { it.name },
            GroupFilter.IF_DIFFERENT,
            false
        )
    }

    val selectWorldItem = item()
        .atSlots(bottomRow + 4)
        .displayAs { createItem(Material.GRASS_BLOCK, name = t("select_world", player)) }
        .onClick {
            SelectWorldInterface.openInventory(
                click.player,
                SelectWorldInterface.SelectWorldContext(
                    context.worldNames,
                    context.onlyOverlappingWorlds
                )
            )
        }

    val groupFilterItem = item()
        .atSlots(bottomRow)
        .displayAs {
            createItem(
                Material.SPYGLASS,
                name = t(
                    if (context.groupFilter == GroupFilter.IF_DIFFERENT) "change_to_all" else "change_to_different",
                    player
                )
            )
        }
        .modifyContext {
            context.also {
                it.groupFilter = when (it.groupFilter) {
                    GroupFilter.ALL -> GroupFilter.IF_DIFFERENT
                    GroupFilter.IF_DIFFERENT -> GroupFilter.ALL
                }
            }
        }

    override fun getOtherItems(): List<InterfaceItem<SelectGroupContext>> {
        return listOf(
            selectWorldItem,
            groupFilterItem
        )
    }

    override fun getInventory(player: Player, context: SelectGroupContext): Inventory {
        return Bukkit.createInventory(
            null,
            6 * 9,
            t("select_group_interface", player, "row" to (context.position + 1).toString())
        )
    }
}