package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persiste granjas colocadas por mundo en: <dataFolder>/farms/worlds/<world>.yml
 * Estructura YAML:
 *
 * version: 1
 * farms:
 *   "x,y,z":
 *     type: wheat
 *     count: 12
 *
 * Nota: La "type" debe existir en FarmsConfig (wheat, carrot, etc).
 */
public final class FarmsManager {

    private static final String RELATIVE_DIR = "farms/worlds"; // NUEVA RUTA
    private static final String KEY_FARMS = "farms";
    private static final String KEY_VERSION = "version";
    private static final int FILE_VERSION = 1;

    private static final Map<World, WorldStore> STORES = new ConcurrentHashMap<>();

    private FarmsManager() {}

    // ─────────────────────────────
    // API principal
    // ─────────────────────────────

    /** Registra / actualiza una farm en una ubicación. */
    public static void setFarm(Location loc, String type, int count) {
        if (!isValidLocation(loc) || type == null || count <= 0) return;
        WorldStore store = getOrLoadStore(loc.getWorld());
        String key = keyOf(loc);
        store.farms.put(key, new FarmData(type, count));
        scheduleSave(store);
    }

    /** Verifica si hay una farm registrada en esa ubicación. */
    public static boolean isFarm(Location loc) {
        if (!isValidLocation(loc)) return false;
        WorldStore store = getOrLoadStore(loc.getWorld());
        return store.farms.containsKey(keyOf(loc));
    }

    /** Elimina una farm en esa ubicación (si existe). */
    public static void removeFarm(Location loc) {
        if (!isValidLocation(loc)) return;
        WorldStore store = getOrLoadStore(loc.getWorld());
        if (store.farms.remove(keyOf(loc)) != null) {
            scheduleSave(store);
        }
    }

    /** Devuelve el conteo de la farm (o 0 si no existe). */
    public static int getFarm(Location loc) {
        FarmData data = getFarmData(loc);
        return data == null ? 0 : data.count();
    }

    /** Devuelve el tipo (wheat, carrot, etc.) o null si no existe. */
    public static String getFarmType(Location loc) {
        FarmData data = getFarmData(loc);
        return data == null ? null : data.type();
    }

    /** Devuelve los datos completos de la farm (type + count) o null si no existe. */
    public static FarmData getFarmData(Location loc) {
        if (!isValidLocation(loc)) return null;
        WorldStore store = getOrLoadStore(loc.getWorld());
        return store.farms.get(keyOf(loc));
    }

    /** Guarda todos los mundos cargados. Útil en onDisable(). */
    public static void saveAllNow() {
        for (WorldStore store : STORES.values()) {
            saveStore(store);
        }
    }

    /** Recarga explícitamente un mundo (opcional). */
    public static void reloadWorld(World world) {
        if (world == null) return;
        WorldStore store = new WorldStore(world);
        loadStore(store);
        WorldStore old = STORES.put(world, store);
        // No borramos el viejo, GC lo recoge; si tenía tarea, la cancelamos por si acaso
        if (old != null && old.scheduledSave != null) {
            old.scheduledSave.cancel();
        }
    }

    // ─────────────────────────────
    // Internos
    // ─────────────────────────────

    private static boolean isValidLocation(Location loc) {
        return loc != null && loc.getWorld() != null;
    }

    private static WorldStore getOrLoadStore(World world) {
        return STORES.computeIfAbsent(world, w -> {
            WorldStore store = new WorldStore(w);
            loadStore(store);
            return store;
        });
    }

    private static void loadStore(WorldStore store) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(store.file);

        // Migración / versión
        int version = yaml.getInt(KEY_VERSION, FILE_VERSION);
        if (version != FILE_VERSION) {
            // Si necesitas migraciones en un futuro, manéjalas aquí
            yaml.set(KEY_VERSION, FILE_VERSION);
        }

        store.farms.clear();
        ConfigurationSection sec = yaml.getConfigurationSection(KEY_FARMS);
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection fs = sec.getConfigurationSection(key);
                if (fs == null) continue;
                String type = fs.getString("type");
                int count = Math.max(0, fs.getInt("count", 0));
                if (type != null && count > 0) {
                    store.farms.put(key, new FarmData(type, count));
                }
            }
        }
    }

    private static void scheduleSave(WorldStore store) {
        // Debounce: guardar 1 tick después (o el delay que quieras)
        if (store.scheduledSave != null) {
            return; // ya hay una pendiente
        }
        store.scheduledSave = Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            saveStore(store);
            store.scheduledSave = null;
        }, 1L);
    }

    private static void saveStore(WorldStore store) {
        try {
            if (!store.file.getParentFile().exists()) {
                store.file.getParentFile().mkdirs();
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(KEY_VERSION, FILE_VERSION);

            if (!store.farms.isEmpty()) {
                YamlConfiguration farmsSec = new YamlConfiguration();
                for (Map.Entry<String, FarmData> e : store.farms.entrySet()) {
                    String key = e.getKey();
                    FarmData data = e.getValue();
                    farmsSec.set(key + ".type", data.type());
                    farmsSec.set(key + ".count", data.count());
                }
                // Copiar farmsSec en yaml bajo "farms"
                for (String path : farmsSec.getKeys(true)) {
                    yaml.set(KEY_FARMS + "." + path, farmsSec.get(path));
                }
            } else {
                yaml.set(KEY_FARMS, null);
            }

            yaml.save(store.file);
        } catch (IOException ex) {
            Bukkit.getLogger().severe("[PhoenixPrisonCore] Failed to save farms for world '" +
                    store.world.getName() + "': " + ex.getMessage());
        }
    }

    private static String keyOf(Location loc) {
        // guardamos coordenadas enteras del bloque
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    // ─────────────────────────────
    // Modelos internos
    // ─────────────────────────────

    public record FarmData(String type, int count) {}

    private static final class WorldStore {
        final World world;
        final File file;
        final Map<String, FarmData> farms = new ConcurrentHashMap<>();
        BukkitTask scheduledSave;

        WorldStore(World world) {
            this.world = world;
            File dataFolder = PhoenixPrisonCore.getInstance().getDataFolder();
            this.file = new File(new File(dataFolder, RELATIVE_DIR), world.getName() + ".yml");
        }
    }
}
