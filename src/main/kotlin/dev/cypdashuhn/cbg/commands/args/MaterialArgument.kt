package dev.cypdashuhn.cbg.commands.args

import dev.cypdashuhn.rooster.common.util.tSend
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.TextArgument
import org.bukkit.Material

private val allowedMaterials = Material.entries
    .filter { it.isBlock }
    .toMutableList()
    .also { it.add(Material.AIR) }
    .map { it.name.lowercase() }

fun materialListArgument(
    key: String = "materials",
    materialInvalidKey: String = "material_invalid",
    materialMissingKey: String = "material_missing"
): Argument<List<Material>> {
    return CustomArgument(TextArgument(key)) { info ->
        val materials = mutableListOf<String>()
        val materialList = mutableListOf<Material>()

        info.input.split(",").forEach { material ->
            try {
                val mat = Material.valueOf(material.uppercase())

                if (material.lowercase() in materials) {
                    info.sender.tSend("cbg.error.material_duplicate", "material" to material)
                    throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                        CustomArgument.MessageBuilder("Duplicate material: ").appendArgInput()
                    )
                }

                materials.add(material.lowercase())
                materialList.add(mat)
            } catch (e: IllegalArgumentException) {
                info.sender.tSend("cbg.error.material_invalid", "material" to material)
                throw CustomArgument.CustomArgumentException.fromMessageBuilder(
                    CustomArgument.MessageBuilder("Invalid material: $material in ").appendArgInput()
                )
            }
        }

        materialList.toList()
    }.replaceSuggestions(
        ArgumentSuggestions.strings { info ->
            val arg = info.currentArg
            val base = arg.substringBeforeLast(",", "").trim()

            if (base.isEmpty()) {
                allowedMaterials
                    .filter { it.startsWith(arg.trim()) }
                    .toTypedArray()
            } else {
                val currentMaterials = base.lowercase().split(",")

                allowedMaterials
                    .filter { material -> currentMaterials.none { material == it } }
                    .map { "$base,$it" }
                    .filter { it.startsWith(arg) }
                    .toTypedArray()
            }
        }
    )
}
