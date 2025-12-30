package dev.cypdashuhn.cbg

import com.google.common.cache.CacheBuilder
import dev.cypdashuhn.cbg.database.GroupManager
import dev.cypdashuhn.cbg.commands.registerCbgCommand
import dev.cypdashuhn.rooster.common.RoosterCache
import dev.cypdashuhn.rooster.common.RoosterServices
import dev.cypdashuhn.rooster.common.initRooster
import dev.cypdashuhn.rooster.db.db
import dev.cypdashuhn.rooster.db.utility_tables.LocationManager
import dev.cypdashuhn.rooster.db.utility_tables.PlayerManager
import dev.cypdashuhn.rooster.db.utility_tables.attributes.PlayerAttributeManager
import dev.cypdashuhn.rooster.localization.provider.LocaleProvider
import dev.cypdashuhn.rooster.localization.provider.YmlLocaleProvider
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.util.UTF8ResourceBundleControl
import org.bukkit.plugin.java.JavaPlugin
import java.util.Locale
import java.util.ResourceBundle
import java.util.concurrent.TimeUnit

class CustomBlockGroups : JavaPlugin() {
    //<editor-fold desc="Services">
    companion object {
        lateinit var plugin: JavaPlugin
        val services = RoosterServices()
        val cache = RoosterCache<String, Any>(CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES))
        val playerManager by services.setDelegate(PlayerManager())
        val playerAttributeManager by services.setDelegate(PlayerAttributeManager(playerManager))
        val locationManager by services.setDelegate(LocationManager())
    }
    //</editor-fold>

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this).verboseOutput(false)) // Load with verbose output
    }

    override fun onEnable() {
        plugin = this

        initLocalizationStore()

        initRooster(plugin, services, cache) {
            services.setDelegate<LocaleProvider>(localeProvider)

            db(listOf(GroupManager.Groups))
        }

        CommandAPI.onEnable()
        registerCbgCommand()
    }

    override fun onDisable() {
        CommandAPI.onDisable()
    }

    //<editor-fold desc="Localization">
    val localeProvider =
        YmlLocaleProvider(
            mapOf(
                "en_US" to Locale.ENGLISH,
                "de_DE" to Locale.GERMAN
            ), "en_US"
        )

    fun initLocalizationStore() {
        val store: MiniMessageTranslationStore = MiniMessageTranslationStore.create(Key.key("namespace:value"))

        val bundle = ResourceBundle.getBundle("dev.cypdashuhn.cbg", Locale.US, UTF8ResourceBundleControl.get())
        store.registerAll(Locale.US, bundle, true)
        GlobalTranslator.translator().addSource(store)
    }
    //</editor-fold>
}

val globalAsDefault = CustomBlockGroups.plugin.config.get("globalAsDefault", true) as Boolean
val datapackName = CustomBlockGroups.plugin.config.getString("datapackName", "cbg")

//<editor-fold desc="Permission Keys">
val canCreate = "cbg_create"
val canDelete = "cbg_delete"
val canSee = "cbg_see"
val canEdit = "cbg_edit"
val canSeeAdminHelp = "cbg_admin_help"
//</editor-fold>