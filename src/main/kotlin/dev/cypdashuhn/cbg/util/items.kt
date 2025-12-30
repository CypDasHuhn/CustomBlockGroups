package dev.cypdashuhn.cbg.util

import dev.cypdashuhn.rooster.common.util.createItem
import dev.cypdashuhn.rooster.localization.t
import dev.cypdashuhn.rooster.ui.interfaces.Context
import dev.cypdashuhn.rooster.ui.interfaces.InterfaceInfo
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

fun <T : Context> pagerItem(extraDescription: InterfaceInfo<T>.() -> List<TextComponent> = { listOf() }): (InterfaceInfo<T>) -> ItemStack =
    {
        createItem(
            Material.COMPASS,
            t("pager", it.player),
            description = listOf(
                t("pager_description_left", it.player),
                t("pager_description_right", it.player),
                *extraDescription(it).toTypedArray()
            )
        )
    }