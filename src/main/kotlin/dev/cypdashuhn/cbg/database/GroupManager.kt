package dev.cypdashuhn.cbg.database

import dev.cypdashuhn.cbg.ui.SelectGroupInterface
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

object GroupManager {
    object Groups : IntIdTable() {
        val name = varchar("name", 64)
        val worldName = varchar("world_name", 64)
        val newMaterials = text("new_materials").nullable()
        val oldMaterials = text("old_materials").nullable()
    }

    class Group(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Group>(Groups)

        var name by Groups.name
        var worldName by Groups.worldName
        var newMaterials by Groups.newMaterials.transformList()
        var oldMaterials by Groups.oldMaterials.transformList()

        fun Column<String?>.transformList() = this.transform(
            { it?.joinToString(",") }, // Serialize
            {
                it?.split(",")?.mapNotNull { materialString ->
                    runCatching { Material.valueOf(materialString) }.getOrNull() // Deserialize with error handling
                }
            }
        )


        fun toDTO() = SelectGroupInterface.GroupDTO(name, worldName, oldMaterials, newMaterials)
    }

    fun addGroup(name: String, worldNames: List<String>, materialList: List<Material>) {
        transaction {
            worldNames.forEach { worldName ->
                Group.new {
                    this.name = name
                    this.worldName = worldName
                    this.newMaterials = materialList
                }
                DatapackManager.createOrOverrideDatapackEntry(name, worldName, materialList)
            }
        }
    }

    fun getGroupsAllMatching(worldNames: List<String>): List<Group> {
        return transaction {
            Group.all()
                .groupBy { it.name }
                .filter { (_, groups) ->
                    val groupWorldNames = groups.map { it.worldName }.toSet()
                    worldNames.all { it in groupWorldNames }
                }
                .map { it.value.first() }
        }
    }

    fun getGroups(worldNames: List<String>): List<Group> {
        return transaction {
            Group.find { Groups.worldName inList worldNames }.toList()
        }
    }

    fun deleteGroup(groupName: String, worldNames: List<String>) {
        transaction {
            Group.find { Groups.name eq groupName and (Groups.worldName inList worldNames) }.forEach { it.delete() }
            worldNames.forEach { worldName ->
                DatapackManager.deleteDatapackEntry(groupName, worldName)
            }
        }
    }

    fun modifyGroup(groupName: String, worldNames: List<String>, materials: List<Material>) {
        transaction {
            Group.find { Groups.name eq groupName and (Groups.worldName inList worldNames) }.forEach {
                if (materials == it.oldMaterials) {
                    it.newMaterials = null
                } else {
                    it.newMaterials = materials
                }
            }
            worldNames.forEach { worldName ->
                DatapackManager.createOrOverrideDatapackEntry(groupName, worldName, materials)
            }
        }
    }

    fun reloadMaterials() {
        transaction {
            Group.all().forEach {
                if (it.newMaterials != null) {
                    it.oldMaterials = it.newMaterials!!
                    it.newMaterials = null
                }
            }
        }
    }

    fun onEnable() {
        reloadMaterials()
    }

    fun updateFromInventory(
        row: Int,
        items: List<ItemStack?>,
        groupName: String,
        worldNames: List<String>
    ): List<Material> {
        lateinit var returnMaterials: List<Material>
        transaction {
            worldNames.forEach {
                val itemsWithId = items.withIndex().map { it.value to it.index + (row * 9) }
                val group = Group.find { Groups.name eq groupName and (Groups.worldName eq it) }.first()
                val materials = (group.newMaterials ?: group.oldMaterials!!)
                val materialsWithId = materials.withIndex()

                val newMaterials = materialsWithId
                    .filter { it.index !in itemsWithId.map { it.second } }
                    .map { it.value }
                    .toMutableList()

                itemsWithId.forEach {
                    if (it.first != null && it.first!!.type.isBlock) {
                        newMaterials.add(it.first!!.type)
                    }
                }
                if (materials.contains(Material.AIR) || materials.isEmpty()) {
                    newMaterials.add(Material.AIR)
                }

                modifyGroup(groupName, listOf(it), newMaterials.distinct())

                returnMaterials = newMaterials.distinct()
            }
        }
        return returnMaterials
    }

    fun changeAirStatus(useAir: Boolean, groupName: String, worldNames: List<String>): List<Material> {
        lateinit var returnMaterials: List<Material>
        transaction {
            worldNames.forEach {
                val group = Group.find { Groups.name eq groupName and (Groups.worldName eq it) }.first()
                val materials = (group.newMaterials ?: group.oldMaterials!!).toMutableList()

                if (useAir) {
                    materials.add(Material.AIR)
                } else {
                    materials.remove(Material.AIR)
                }

                returnMaterials = materials

                modifyGroup(groupName, listOf(it), materials.distinct())
            }
        }

        return returnMaterials
    }

    fun findGroupsForInterface(
        worldNames: List<String>,
        groupFilter: SelectGroupInterface.GroupFilter,
        onlyOverlappingWorlds: Boolean,
        offset: Int
    ): List<SelectGroupInterface.GroupDTO>? {
        return transaction {
            val validGroupNames = Groups
                .select(Groups.name, Groups.worldName)
                .where { Groups.worldName inList worldNames }
                .groupBy(Groups.name)
                .having {
                    Groups.worldName.count() greaterEq worldNames.size.toLong()
                }.map { it[Groups.name] }

            val base = if (onlyOverlappingWorlds) {
                Groups
                    .selectAll()
                    .where {
                        (Groups.name inList validGroupNames) and
                                (Groups.worldName inList worldNames)
                    }
            } else {
                Groups.selectAll().where { Groups.worldName inList worldNames }
            }

            when (groupFilter) {
                SelectGroupInterface.GroupFilter.ALL -> listOf(
                    Group.wrapRow(
                        base.orderBy(Groups.name to SortOrder.DESC).limit(1, offset.toLong()).firstOrNull()
                            ?: return@transaction null
                    ).toDTO()
                )

                SelectGroupInterface.GroupFilter.IF_DIFFERENT -> {
                    val groups = base
                        .orderBy(Groups.name to SortOrder.DESC)
                        .groupBy(Groups.newMaterials, Groups.oldMaterials)

                    val entry = groups.limit(1, offset.toLong()).firstOrNull() ?: return@transaction null

                    var query = (Groups.worldName inList worldNames) and
                            (Groups.newMaterials eq entry[Groups.newMaterials]) and
                            (Groups.oldMaterials eq entry[Groups.oldMaterials])

                    if (onlyOverlappingWorlds) query =
                        query and (Groups.name inList validGroupNames)

                    val sameGroupEntries = Groups.selectAll()
                        .where {
                            query
                        }
                        .orderBy(Groups.name to SortOrder.DESC)
                        .map { Group.wrapRow(it).toDTO() }

                    return@transaction sameGroupEntries
                }
            }
        }
    }
}