package dev.cypdashuhn.cbg.ui

import dev.cypdashuhn.cbg.canEdit
import dev.cypdashuhn.cbg.database.GroupManager
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
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.reflect.KClass

object GroupInterface : ScrollInterface<GroupInterface.GroupContext, Material>(
    "GroupInterface",
    options {
        modifyScroller = {
            val action = this.state.onClick
            this.run {
                onClick {
                    if (!context.isEditing) action?.invoke(this)
                    else event.isCancelled = true
                }
                .displayAs(pagerItem { if (context.isEditing) listOf(t("pager_disabled_editing", player)) else listOf()})
            }
        }

        modifyContentItem = {
            this.run {
                onClick {
                    event.isCancelled = !context.isEditing
                }
            }
        }

        modifyClickInArea = {
            this.run {
                onClick {
                    event.isCancelled = !context.isEditing
                }
            }
        }
    }
) {
    class GroupContext(
        var groups: List<SelectGroupInterface.GroupDTO>,
        var useOldMaterial: Boolean = false,
        var isEditing: Boolean = false
    ) : ScrollContext() {
        val materials = groups.first().newMaterials ?: groups.first().oldMaterials!!
    }

    override fun contentProvider(id: Int, context: GroupContext): Material? {
        val first = context.groups.first() /* The list is either singular, or grouped by materials. */

        val material = if (context.useOldMaterial) {
            first.oldMaterials ?: first.newMaterials!!
        } else first.newMaterials ?: first.oldMaterials!!

        return material.getOrNull(id)
    }

    override val contextClass: KClass<GroupContext> get() = GroupContext::class

    override fun defaultContext(player: Player): GroupContext {
        // This is never called.
        throw IllegalStateException("There is no default Context for Group Interface!")
    }

    override fun contentDisplay(
        data: Material,
        context: GroupContext
    ): InterfaceInfo<GroupContext>.() -> ItemStack = { ItemStack(data) }

    override fun contentClick(
        data: Material,
        context: GroupContext
    ): ClickInfo<GroupContext>.() -> Unit = { }

    //<editor-fold desc="Items">
    val backToSelectionItem = item()
        .atSlots(bottomRow)
        .displayAs { createItem(Material.FEATHER, name = t("back_to_selection", player)) }
        .onClick { SelectGroupInterface.openInventory(click.player) }

    val worldsItem = item()
        .atSlots(bottomRow + 1)
        .displayAs {
            val description = context.groups.map { minimessage("<dark_purple>${it.worldName}") }

            createItem(Material.GRASS_BLOCK, name = t("worlds", player), description = description)
        }

    val isEditingItem = item()
        .atSlots(bottomRow + 3)
        .usedWhen { context.isEditing && !context.useOldMaterial && player.hasPermission(canEdit) }
        .displayAs {
            val description = listOf(t(if (context.isEditing) "on" else "off", player))
            createItem(Material.WRITABLE_BOOK, name = t("is_editing", player), description = description)
        }
        .modifyContext { context.isEditing = true }

    val cancelItem = item()
        .atSlots(bottomRow + 3)
        .usedWhen { context.isEditing }
        .displayAs { createItem(Material.RED_STAINED_GLASS_PANE, name = t("cancel", player)) }
        .modifyContext { context.isEditing = false }

    val saveItem = item()
        .atSlots(bottomRow + 5)
        .usedWhen { context.isEditing }
        .displayAs { createItem(Material.GREEN_STAINED_GLASS_PANE, name = t("save", player)) }
        .onClick {
            val items = (0..bottomRow).map {
                event.inventory.getItem(it)
            }
            val newMaterials = GroupManager.updateFromInventory(
                context.position,
                items,
                context.groups.first().name,
                context.groups.map { it.worldName }
            )

            openInventory(click.player, context.also {
                it.isEditing = false
                it.groups = it.groups.map { it.also { it.newMaterials = newMaterials } }
            })
        }

    val useOldMaterialItem = item()
        .atSlots(bottomRow + 5)
        .usedWhen { !context.isEditing && context.groups.first().oldMaterials != context.groups.first().newMaterials }
        .displayAs {
            val description = listOf(t(if (context.useOldMaterial) "on" else "off", player))

            createItem(Material.KNOWLEDGE_BOOK, name = t("use_old_material", player), description = description)
        }
        .modifyContext { context.useOldMaterial = !context.useOldMaterial }

    val useAirItem = item()
        .atSlots(bottomRow + 7)
        .displayAs {
            val materials = if (context.useOldMaterial) context.groups.first().oldMaterials
                ?: context.groups.first().newMaterials!! else context.groups.first().newMaterials
                ?: context.groups.first().oldMaterials!!

            val useAir = materials.contains(Material.AIR)
            val description = mutableListOf(t(if (useAir) "on" else "off", player))

            if (context.useOldMaterial) description.add(t("use_air_old", player))
            if (context.isEditing) (description.add(t("use_air_editing", player)))
            if (context.materials.first() == Material.AIR && context.materials.size == 1) {
                description.add(t("use_air_last", player))
            }
            if (!player.hasPermission(canEdit)) {
                description.add(t("use_air_permissionless", player))
            }
            createItem(Material.BARRIER, name = t("use_air", player), description = description)
        }
        .modifyContext {
            val canEdit = !context.useOldMaterial &&
                    !context.isEditing &&
                    !(context.materials.first() == Material.AIR && context.materials.size == 1) &&
                    click.player.hasPermission(canEdit)

            if (canEdit) {
                val useAir = (context.groups.first().newMaterials
                    ?: context.groups.first().oldMaterials!!).contains(Material.AIR)
                val newMaterials = GroupManager.changeAirStatus(
                    !useAir,
                    context.groups.first().name,
                    context.groups.map { it.worldName })

                context.also { it.groups.forEach { it.newMaterials = newMaterials } }
            }
        }

    override fun getOtherItems(): List<InterfaceItem<GroupContext>> {
        return listOf(
            backToSelectionItem,
            worldsItem,
            isEditingItem,
            cancelItem,
            saveItem,
            useOldMaterialItem,
            useAirItem
        )
    }
    //</editor-fold>

    override fun getInventory(player: Player, context: GroupContext): Inventory {
        return Bukkit.createInventory(
            null,
            6 * 9,
            t(
                "group_interface",
                player,
                "row" to (context.position + 1).toString(),
                "groupName" to context.groups.first().name
            )
        )
    }
}