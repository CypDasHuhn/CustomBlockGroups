package dev.cypdashuhn.cbg.database

import com.google.gson.Gson
import com.google.gson.JsonObject
import dev.cypdashuhn.cbg.datapackName
import org.bukkit.Bukkit
import org.bukkit.Material
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

/**
 * Manages migration and synchronization between the database and datapack files.
 * Provides bidirectional migration to handle data consistency.
 */
object MigrationManager {

    /**
     * Resets all datapack folders and recreates them using the database as the source of truth.
     * This will:
     * 1. Clear all existing datapack group entries
     * 2. Recreate all datapack entries from database groups
     * 3. Ensure pack.mcmeta files exist
     *
     * Use this when the database is correct and datapacks need to be rebuilt.
     */
    fun migrateFromDatabase() {
        transaction {
            // Get all worlds
            val worlds = Bukkit.getWorlds().map { it.name }

            // Clear all existing datapack group entries
            worlds.forEach { worldName ->
                val datapackPath = DatapackManager.getDatapackPath(worldName)
                val blocksDirectory = datapackPath
                    .resolve("data/$datapackName/tags")
                    .resolve(DatapackManager.blockFolder)

                // Delete all existing group files
                if (blocksDirectory.exists()) {
                    blocksDirectory.listFiles()?.forEach { file ->
                        if (file.extension == "json") {
                            file.delete()
                        }
                    }
                }
            }

            // Recreate datapacks from database
            GroupManager.Group.all().forEach { group ->
                val materials = group.newMaterials ?: group.oldMaterials
                if (materials != null) {
                    DatapackManager.createOrOverrideDatapackEntry(
                        group.name,
                        group.worldName,
                        materials
                    )
                }
            }
        }

        // Ensure pack.mcmeta files exist
        DatapackManager.initializeDatapacks()
    }

    /**
     * Resets the database and repopulates it using datapack files as the source of truth.
     * This will:
     * 1. Clear all existing database entries
     * 2. Scan all datapack folders in all worlds
     * 3. Parse JSON files and recreate database groups
     *
     * Use this when datapacks are correct and the database needs to be rebuilt.
     *
     * Note: This migration assumes that datapack files follow the standard format:
     * {"values": ["minecraft:material_name", ...]}
     */
    fun migrateFromDatapacks() {
        transaction {
            // Clear all existing database entries
            GroupManager.Groups.deleteAll()

            // Get all worlds
            val worlds = Bukkit.getWorlds().map { it.name }
            val gson = Gson()

            // Scan each world's datapacks
            worlds.forEach { worldName ->
                val datapackPath = DatapackManager.getDatapackPath(worldName)
                val blocksDirectory = datapackPath
                    .resolve("data/$datapackName/tags")
                    .resolve(DatapackManager.blockFolder)

                // Read all group files
                if (blocksDirectory.exists()) {
                    blocksDirectory.listFiles()?.forEach { file ->
                        if (file.extension == "json") {
                            try {
                                val groupName = file.nameWithoutExtension
                                val jsonContent = file.readText()
                                val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)

                                // Parse materials from the values array
                                val materials = jsonObject.getAsJsonArray("values")
                                    ?.mapNotNull { element ->
                                        val materialString = element.asString
                                            .removePrefix("minecraft:")
                                            .uppercase()

                                        runCatching { Material.valueOf(materialString) }.getOrNull()
                                    } ?: emptyList()

                                // Create database entry
                                if (materials.isNotEmpty()) {
                                    GroupManager.Group.new {
                                        this.name = groupName
                                        this.worldName = worldName
                                        this.oldMaterials = materials
                                        this.newMaterials = null
                                    }
                                }
                            } catch (e: Exception) {
                                Bukkit.getLogger().warning(
                                    "Failed to parse datapack file: ${file.absolutePath} - ${e.message}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
