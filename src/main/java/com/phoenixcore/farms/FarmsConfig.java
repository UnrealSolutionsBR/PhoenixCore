package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.*;

/**
 * Loads and serves data from farms/farms.yml
 * File is packaged at: src/main/resources/farms/farms.yml
 * On first run it will be copied to: <plugin data folder>/farms/farms.yml
 */
public class FarmsConfig {

    // Path inside the JAR and inside the data folder
    private static final String FILE_PATH = "farms/farms.yml";

    private static FarmsConfig INSTANCE;

    public static FarmsConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new FarmsConfig();
        }
        return INSTANCE;
    }

    // PDC key to mark farm items with their farm "type" (wheat, carrot, ...)
    private final NamespacedKey PDC_FARM_TYPE =
            new NamespacedKey(PhoenixPrisonCore.getInstance(), "farm_type");

    // Global config
    private int stackLimit;
    private int regrowDelayTicks;
    private boolean validateDisplayName;

    // farmType -> FarmDef
    private final Map<String, FarmDef> farms = new HashMap<>();

    private FarmsConfig() {
        load();
    }

    public void reload() {
        farms.clear();
        load();
    }

    private void load() {
        PhoenixPrisonCore plugin = PhoenixPrisonCore.getInstance();

        // Ensure the file exists in the data folder; copy from JAR if missing
        File target = new File(plugin.getDataFolder(), FILE_PATH);
        if (!target.exists()) {
            // Create parent dirs (plugins/YourPlugin/farms/)
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            plugin.saveResource(FILE_PATH, false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(target);

        // Global settings
        this.stackLimit = cfg.getInt("global.stack_limit", 64);
        this.regrowDelayTicks = cfg.getInt("global.regrow_delay_ticks", 100);
        this.validateDisplayName = cfg.getBoolean("global.validate_item_display_name", true);

        // Farms
        ConfigurationSection farmsSec = cfg.getConfigurationSection("farms");
        if (farmsSec != null) {
            for (String key : farmsSec.getKeys(false)) {
                ConfigurationSection fs = farmsSec.getConfigurationSection(key);
                if (fs == null) continue;

                String itemName = fs.getString("item.name", "§eFarm");
                List<String> itemLore = fs.getStringList("item.lore");

                Material baseBlock = safeMat(fs.getString("base_block"), Material.HAY_BLOCK);
                Material cropBlock = safeMat(fs.getString("crop_block"), Material.WHEAT);

                ConfigurationSection rewardSec = fs.getConfigurationSection("reward");
                Material rewardType = Material.WHEAT;
                int rewardAmount = 1;
                if (rewardSec != null) {
                    rewardType = safeMat(rewardSec.getString("type"), Material.WHEAT);
                    rewardAmount = Math.max(1, rewardSec.getInt("amount_per_farm", 1));
                }

                FarmDef def = new FarmDef(
                        key,
                        itemName,
                        itemLore == null ? Collections.emptyList() : itemLore,
                        baseBlock,
                        cropBlock,
                        rewardType,
                        rewardAmount
                );

                farms.put(key.toLowerCase(Locale.ROOT), def);
            }
        }
    }

    private Material safeMat(String name, Material def) {
        if (name == null) return def;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    // ─────────────────────────────
    // Public API
    // ─────────────────────────────

    public int getStackLimit() {
        return stackLimit;
    }

    public int getRegrowDelayTicks() {
        return regrowDelayTicks;
    }

    public boolean isValidateDisplayName() {
        return validateDisplayName;
    }

    public FarmDef getFarm(String type) {
        if (type == null) return null;
        return farms.get(type.toLowerCase(Locale.ROOT));
    }

    public Collection<FarmDef> allFarms() {
        return farms.values();
    }

    /**
     * Checks if an ItemStack is a valid farm item produced by this plugin:
     * - Has PDC farm_type
     * - (Optional) display name matches the configured one
     */
    public boolean isFarmItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(PDC_FARM_TYPE, PersistentDataType.STRING);
        if (type == null) return false;

        FarmDef def = getFarm(type);
        if (def == null) return false;

        if (validateDisplayName) {
            return def.itemName().equals(meta.getDisplayName());
        }
        return true;
    }

    /**
     * Returns the farm "type" stored in the item's PDC, or null if not present.
     */
    public String getFarmType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(PDC_FARM_TYPE, PersistentDataType.STRING);
    }

    /**
     * Builds an ItemStack representing a farm of the given type and amount.
     * The stack uses the configured base_block as the material, sets display name and lore,
     * and marks PDC "farm_type" for safe validation.
     */
    public ItemStack buildFarmItem(String type, int amount) {
        FarmDef def = getFarm(type);
        if (def == null) return null;

        ItemStack stack = new ItemStack(def.baseBlock(), Math.max(1, amount));
        ItemMeta im = stack.getItemMeta();
        if (im != null) {
            im.setDisplayName(ChatColor.translateAlternateColorCodes('&', def.itemName()));
            if (def.itemLore() != null && !def.itemLore().isEmpty()) {
                List<String> lore = def.itemLore().stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .toList();
                im.setLore(lore);
            }
            im.getPersistentDataContainer().set(PDC_FARM_TYPE, PersistentDataType.STRING, def.type());
            stack.setItemMeta(im);
        }
        return stack;
    }

    public NamespacedKey pdcKeyFarmType() {
        return PDC_FARM_TYPE;
    }

    // ─────────────────────────────
    // Model
    // ─────────────────────────────

    /**
     * A farm definition loaded from YAML.
     */
    public record FarmDef(
            String type,
            String itemName,
            List<String> itemLore,
            Material baseBlock,
            Material cropBlock,
            Material rewardType,
            int rewardAmountPerFarm
    ) {}
}
