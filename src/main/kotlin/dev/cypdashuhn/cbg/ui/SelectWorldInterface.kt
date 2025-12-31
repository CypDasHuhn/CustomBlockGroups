package dev.cypdashuhn.cbg.ui

import dev.cypdashuhn.cbg.commands.defaultWorlds
import dev.cypdashuhn.cbg.util.minimessage
import dev.cypdashuhn.cbg.util.pagerItem
import dev.cypdashuhn.rooster.common.util.createItem
import dev.cypdashuhn.rooster.localization.t
import dev.cypdashuhn.rooster.ui.interfaces.ClickInfo
import dev.cypdashuhn.rooster.ui.interfaces.InterfaceInfo
import dev.cypdashuhn.rooster.ui.interfaces.constructors.indexed_content.ScrollContext
import dev.cypdashuhn.rooster.ui.interfaces.constructors.indexed_content.ScrollInterface
import dev.cypdashuhn.rooster.ui.interfaces.options
import dev.cypdashuhn.rooster.ui.items.InterfaceItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.reflect.KClass

object SelectWorldInterface : ScrollInterface<SelectWorldInterface.SelectWorldContext, World>(
    "SelectWorldInterface",
    options {
        contentArea = (0 to 0) to (8 to 0)
        modifyScroller = { displayAs(pagerItem()) }

        inventorySize = 2 * 9
        inventoryTitle = { player, context -> t("select_world_interface", player, "row" to (context.position + 1).toString()) }
    }
) {
    //<editor-fold desc="Context">
    override val contextClass: KClass<SelectWorldContext> get() = SelectWorldContext::class

    class SelectWorldContext(
        var selectedWorldNames: List<String>,
        var onlyOverlappingWorlds: Boolean
    ) : ScrollContext()
    //</editor-fold>

    //<editor-fold desc="Helper">
    fun worldBlockFromName(name: String): Material {
        if (name.contains("nether", ignoreCase = true)) return Material.NETHERRACK
        if (name.contains("end", ignoreCase = true)) return Material.END_STONE
        return Material.GRASS_BLOCK
    }
    //</editor-fold>

    //<editor-fold desc="Content">
    override fun contentProvider(id: Int, context: SelectWorldContext): World? {
        return Bukkit.getWorlds().getOrNull(id)
    }


    override fun defaultContext(player: Player): SelectWorldContext {
        return SelectWorldContext(selectedWorldNames = defaultWorlds(player).map { it.name }, false)
    }

    override fun contentDisplay(
        data: World,
        context: SelectWorldContext
    ): InterfaceInfo<SelectWorldContext>.() -> ItemStack = {
        val isOn = context.selectedWorldNames.contains(data.name)

        createItem(
            worldBlockFromName(data.name),
            name = minimessage("<yellow>${data.name}"),
            description = listOf(t(if (isOn) "on" else "off", player)),
            additional = {
                it.setEnchantmentGlintOverride(isOn)
            }
        )
    }

    override fun contentClick(
        data: World,
        context: SelectWorldContext
    ): ClickInfo<SelectWorldContext>.() -> Unit = {
        val isOn = context.selectedWorldNames.contains(data.name)

        context.selectedWorldNames = if (isOn) {
            if (context.selectedWorldNames.size > 1) {
                context.selectedWorldNames - data.name
            } else context.selectedWorldNames
        } else {
            context.selectedWorldNames + data.name
        }
        openInventory(click.player, context)
    }
    //</editor-fold>

    //<editor-fold desc="Items">
    val backToSelectionItem = item()
        .atSlots(bottomRow + 9)
        .displayAs { createItem(Material.FEATHER, name = t("back_to_selection", player)) }
        .routeTo(SelectGroupInterface) {
            val context = SelectGroupInterface.getContext(click.player)
            context.onlyOverlappingWorlds = this.context.onlyOverlappingWorlds
            context.worldNames = this.context.selectedWorldNames
            context
        }

    val onlyOverlappingWorldsItem = item()
        .atSlots(bottomRow + 6)
        .displayAs { createItem(Material.REPEATER, name = t("only_overlapping_worlds", player)) }
        .modifyContext { context.onlyOverlappingWorlds = !context.onlyOverlappingWorlds }

    override fun getOtherItems(): List<InterfaceItem<SelectWorldContext>> {
        return listOf(
            backToSelectionItem,
            onlyOverlappingWorldsItem
        )
    }
    //</editor-fold>
}