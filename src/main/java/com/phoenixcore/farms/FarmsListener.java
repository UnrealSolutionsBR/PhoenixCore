package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
import com.phoenixcore.locale.LocaleManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map;

public class FarmsListener implements Listener {

    // ───────── Nuevo: cooldown por jugador para el mensaje de "debes agacharte"
    private static final long COOLDOWN_MS = 5000L;
    private final Map<UUID, Long> lastSneakWarn = new HashMap<>();

    // ─────────────────────────────
    // Colocar farm normal (1x) → solo si el ítem es una farm válida (FarmsConfig)
    // ─────────────────────────────
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block base = event.getBlockPlaced();
        if (base.getType() == Material.AIR) return;

        ItemStack placed = event.getItemInHand();
        if (placed == null || !placed.hasItemMeta()) return;

        FarmsConfig cfg = FarmsConfig.get();

        // Verificar si el ítem es una farm válida (PDC)
        String farmType = cfg.getFarmType(placed);
        // Compatibilidad por nombre histórico
        if (farmType == null) {
            ItemMeta im = placed.getItemMeta();
            if (im != null && "§eWheat Farm".equals(im.getDisplayName()) && cfg.getFarm("wheat") != null) {
                farmType = "wheat";
            }
        }
        if (farmType == null) return;

        FarmsConfig.FarmDef def = cfg.getFarm(farmType);
        if (def == null) return;

        Block above = base.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            event.getPlayer().sendMessage(LocaleManager.getMessage("farms-place-need-air-above"));
            return;
        }

        // Forzar orientación vertical (axis Y) para el bloque base si es orientable
        var data = base.getBlockData();
        if (data instanceof org.bukkit.block.data.Orientable orientable) {
            orientable.setAxis(org.bukkit.Axis.Y);
            base.setBlockData(orientable, false);
        }

        // Registrar farm (1x)
        FarmsManager.setFarm(base.getLocation(), def.type(), 1);

        // Spawn cultivo maduro (o bloque cosechable) tras delay
        int delay = Math.max(1, cfg.getRegrowDelayTicks());
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (base.getType() == def.baseBlock() && above.getType() == Material.AIR) {
                spawnMatureCrop(above, def);
            }
        }, delay);

        event.getPlayer().sendMessage(LocaleManager.getMessage("farms-place-placed-one")
                .replace("%farm_name%", def.itemName()));
    }

    // ─────────────────────────────
    // Shift + Right Click para colocar un stack (usa stack_limit del farms.yml)
    // ─────────────────────────────
    @EventHandler
    public void onInteractPlaceStack(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || !inHand.hasItemMeta()) return;

        FarmsConfig cfg = FarmsConfig.get();
        String farmType = cfg.getFarmType(inHand);
        // Compatibilidad por nombre histórico
        if (farmType == null) {
            ItemMeta im = inHand.getItemMeta();
            if (im != null && "§eWheat Farm".equals(im.getDisplayName()) && cfg.getFarm("wheat") != null) {
                farmType = "wheat";
            }
        }
        if (farmType == null) return;

        FarmsConfig.FarmDef def = cfg.getFarm(farmType);
        if (def == null) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Block placeAt = clicked.getRelative(event.getBlockFace());
        if (placeAt.getType() != Material.AIR) return;

        Block above = placeAt.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            player.sendMessage(LocaleManager.getMessage("farms-place-need-air-above"));
            return;
        }

        int stackLimit = Math.max(1, cfg.getStackLimit());
        int farms = Math.max(1, Math.min(inHand.getAmount(), stackLimit));

        placeAt.setType(def.baseBlock());

        // Forzar orientación vertical (axis Y) si el bloque soporta orientación
        var data = placeAt.getBlockData();
        if (data instanceof org.bukkit.block.data.Orientable orientable) {
            orientable.setAxis(org.bukkit.Axis.Y);
            placeAt.setBlockData(orientable, false);
        }

        FarmsManager.setFarm(placeAt.getLocation(), def.type(), farms);

        // consumir el stack usado
        inHand.setAmount(0);

        int delay = Math.max(1, cfg.getRegrowDelayTicks());
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (placeAt.getType() == def.baseBlock() && above.getType() == Material.AIR) {
                spawnMatureCrop(above, def);
            }
        }, delay);

        player.sendMessage(LocaleManager.getMessage("farms-place-placed-stacked")
                .replace("%count%", String.valueOf(farms))
                .replace("%farm_name%", def.itemName()));
        event.setCancelled(true);
    }

    // ─────────────────────────────
    // Left Click:
    //  - Si clickeas el bloque base → AHORA requiere shift para recoger la farm
    //  - Si clickeas el cultivo encima con una farm debajo → cosechar
    // ─────────────────────────────
    @EventHandler
    public void onLeftClickFarm(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Player player = event.getPlayer();

        // 1) Si es el bloque base de una farm → solo se puede recoger haciendo shift
        if (FarmsManager.isFarm(clicked.getLocation())) {
            event.setCancelled(true);
            if (!player.isSneaking()) {
                warnMustSneak(player);
                return;
            }
            breakFarmAndGiveItem(clicked, player);
            return;
        }

        // 2) Si es el cultivo encima de una farm → cosechar (no requiere shift)
        Block below = clicked.getRelative(0, -1, 0);
        if (FarmsManager.isFarm(below.getLocation())) {
            event.setCancelled(true); // evita rotura vanilla
            harvestCrop(clicked, below, player);
        }
    }

    // ─────────────────────────────
    // Romper bloques:
    //  - Si rompe cultivo encima de una farm → cosechar (no requiere shift)
    //  - Si rompe el bloque base → requiere shift o se cancela con aviso (cooldown)
    // ─────────────────────────────
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Caso A: cultivo Ageable (wheat, carrots, etc.) con base de farm
        if (block.getBlockData() instanceof Ageable) {
            Block base = block.getRelative(0, -1, 0);
            if (!FarmsManager.isFarm(base.getLocation())) return;

            event.setDropItems(false);
            event.setCancelled(true); // manejamos la cosecha manualmente
            harvestCrop(block, base, player);
            return;
        }

        // Caso B: bloque cosechable NO Ageable (pumpkin, melon, etc.) con base de farm
        Block base = block.getRelative(0, -1, 0);
        if (FarmsManager.isFarm(base.getLocation())) {
            event.setDropItems(false);
            event.setCancelled(true); // manejamos la cosecha manualmente
            harvestCrop(block, base, player);
            return;
        }

        // Caso C: romper el bloque base de la farm → requiere shift
        if (FarmsManager.isFarm(block.getLocation())) {
            event.setDropItems(false);
            event.setCancelled(true);
            if (!player.isSneaking()) {
                warnMustSneak(player);
                return;
            }
            breakFarmAndGiveItem(block, player);
        }
    }

    // ─────────────────────────────
    // Evitar que el cultivo Ageable se rompa por física cuando es parte de una farm
    // ─────────────────────────────
    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getBlockData() instanceof Ageable) {
            Block below = block.getRelative(0, -1, 0);
            if (FarmsManager.isFarm(below.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    // ───────── Nuevo: aviso con cooldown y reinicio de ventana
    private void warnMustSneak(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Long last = lastSneakWarn.get(id);

        if (last == null || (now - last) >= COOLDOWN_MS) {
            player.sendMessage(LocaleManager.getMessage("farms-sneak-to-break"));
            lastSneakWarn.put(id, now);
        } else {
            // No mostramos mensaje, pero reiniciamos ventana a partir de este intento
            lastSneakWarn.put(id, now);
        }
    }

    // ─────────────────────────────
    // Cosecha: dar recompensa y regenerar
    // ─────────────────────────────
    private void harvestCrop(Block crop, Block base, Player player) {
        FarmsManager.FarmData data = FarmsManager.getFarmData(base.getLocation());
        if (data == null) return;

        FarmsConfig.FarmDef def = FarmsConfig.get().getFarm(data.type());
        if (def == null) return;

        // Validar que el bloque a cosechar coincide con el configurado o es Ageable
        if (crop.getType() != def.cropBlock() && !(crop.getBlockData() instanceof Ageable)) {
            return;
        }

        // Eliminar el cultivo actual
        crop.setType(Material.AIR);

        // Recompensa total
        int total = Math.max(1, def.rewardAmountPerFarm() * Math.max(1, data.count()));
        ItemStack reward = new ItemStack(def.rewardType(), total);

        // Intentar meter al inventario y soltar lo que no quepa a los pies del jugador
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(reward);
        int dropped = 0;
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                dropped += item.getAmount();
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            player.sendMessage(LocaleManager.getMessage("farms-harvest-inv-partial")
                    .replace("%remaining%", String.valueOf(dropped)));
        }

        // Regenerar tras delay
        int delay = Math.max(1, FarmsConfig.get().getRegrowDelayTicks());
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (base.getType() == def.baseBlock()) {
                Block above = base.getRelative(0, 1, 0);
                if (above.getType() == Material.AIR) {
                    spawnMatureCrop(above, def);
                }
            }
        }, delay);
    }

    // ─────────────────────────────
    // Colocar el cultivo/bloque en estado "maduro"
    // ─────────────────────────────
    private void spawnMatureCrop(Block where, FarmsConfig.FarmDef def) {
        where.setType(def.cropBlock());
        if (where.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            where.setBlockData(ageable);
        }
    }

    // ─────────────────────────────
    // Recoger la farm completa
    // ─────────────────────────────
    private void breakFarmAndGiveItem(Block baseBlock, Player player) {
        FarmsManager.FarmData data = FarmsManager.getFarmData(baseBlock.getLocation());
        if (data == null) return;

        // Bloquear si el inventario está completamente lleno
        if (player.getInventory().firstEmpty() == -1) {
            // Sugerencia: añade este key al locale:
            // farms-break-inv-full: "&cYour inventory is full! Make space to pick up the farm."
            player.sendMessage(LocaleManager.getMessage("farms-break-inv-full"));
            return;
        }

        // Quitar cultivo de arriba si existe
        Block above = baseBlock.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            above.setType(Material.AIR);
        }

        int remaining = Math.max(1, data.count());
        int dropped = 0;

        while (remaining > 0) {
            int give = Math.min(64, remaining);

            ItemStack farmStack = FarmsConfig.get().buildFarmItem(data.type(), give);
            if (farmStack == null) {
                // Fallback de seguridad
                farmStack = new ItemStack(Material.HAY_BLOCK, give);
                ItemMeta meta = farmStack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§eFarm (" + data.type() + ")");
                    meta.setLore(List.of("§7Place this block on the ground"));
                    farmStack.setItemMeta(meta);
                }
            }

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(farmStack);
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    dropped += item.getAmount();
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }

            remaining -= give;
        }

        if (dropped > 0) {
            player.sendMessage(LocaleManager.getMessage("farms-harvest-inv-partial")
                    .replace("%remaining%", String.valueOf(dropped)));
        }

        // Quitar del mundo y del manager
        baseBlock.setType(Material.AIR);
        FarmsManager.removeFarm(baseBlock.getLocation());
    }
}
