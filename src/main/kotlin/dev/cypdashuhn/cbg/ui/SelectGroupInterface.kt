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
import kotlin.reflect.KClass

object SelectGroupInterface :
    ScrollInterface<SelectGroupInterface.SelectGroupContext, List<SelectGroupInterface.GroupDTO>>(
        "SelectGroupInterface",
        options {
            modifyScroller = { displayAs(pagerItem()) }
            inventoryTitle = { player, context -> t("select_group_interface", player, "row" to (context.position + 1).toString()) }
        }
    ) {
    //<editor-fold desc="Classes">
    //<editor-fold desc="Context">
    override val contextClass: KClass<SelectGroupContext> get() = SelectGroupContext::class
    class SelectGroupContext(
        var worldNames: List<String>,
        var groupFilter: GroupFilter,
        var onlyOverlappingWorlds: Boolean
    ) : ScrollContext()

    override fun defaultContext(player: Player): SelectGroupContext {
        return SelectGroupContext(
            worldNames = defaultWorlds(player).map { it.name },
            GroupFilter.IF_DIFFERENT,
            false
        )
    }
    //</editor-fold>

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
    //</editor-fold>

    //<editor-fold desc="Content">
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
            if (materials.first() != Material.AIR) materials.first()
            else materials.getOrNull(1) ?: Material.BARRIER

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
        return GroupManager.findGroupsForInterface(
            context.worldNames,
            context.groupFilter,
            context.onlyOverlappingWorlds,
            id
        )
    }
    //</editor-fold>

    //<editor-fold desc="Items">
    val selectWorldItem = item()
        .atSlots(bottomRow + 4)
        .displayAs { createItem(Material.GRASS_BLOCK, name = t("select_world", player)) }
        .routeTo(SelectWorldInterface) {
            SelectWorldInterface.SelectWorldContext(context.worldNames, context.onlyOverlappingWorlds)
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
            context.groupFilter = when (context.groupFilter) {
                GroupFilter.ALL -> GroupFilter.IF_DIFFERENT
                GroupFilter.IF_DIFFERENT -> GroupFilter.ALL
            }
        }


    override fun getOtherItems(): List<InterfaceItem<SelectGroupContext>> {
        return listOf(
            selectWorldItem,
            groupFilterItem
        )
    }
    //</editor-fold>
}