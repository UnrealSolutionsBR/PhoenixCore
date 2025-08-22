package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
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

import java.util.List;
import java.util.Map;

public class FarmsListener implements Listener {

    // ─────────────────────────────
    // Colocar farm normal (1x) → solo si el ítem es una farm válida (FarmsConfig)
    // ─────────────────────────────
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block base = event.getBlockPlaced();

        // Solo nos importa si el bloque colocado coincide con algún base_block de farms.yml
        if (base.getType() == Material.AIR) return;

        ItemStack placed = event.getItemInHand();
        if (placed == null || !placed.hasItemMeta()) return;

        FarmsConfig cfg = FarmsConfig.get();

        // Verificar si el ítem es una farm válida
        String farmType = cfg.getFarmType(placed);
        // Compatibilidad: si aún no marcas PDC y usas el nombre antiguo, intenta mapear "Wheat Farm" → wheat
        if (farmType == null) {
            ItemMeta im = placed.getItemMeta();
            if (im != null && "§eWheat Farm".equals(im.getDisplayName()) && cfg.getFarm("wheat") != null) {
                farmType = "wheat";
            }
        }
        if (farmType == null) return; // no es una farm del plugin

        FarmsConfig.FarmDef def = cfg.getFarm(farmType);
        if (def == null) return;

        // Verificar que el bloque de arriba esté libre
        Block above = base.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            event.getPlayer().sendMessage("§cYou need an empty block above to place the farm.");
            return;
        }

        // Registrar farm (1x) en el manager con su tipo
        FarmsManager.setFarm(base.getLocation(), def.type(), 1);

        // Spawn del cultivo maduro tras el delay configurado
        int delay = Math.max(1, cfg.getRegrowDelayTicks());
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (base.getType() == def.baseBlock() && above.getType() == Material.AIR) {
                above.setType(def.cropBlock());
                if (above.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    above.setBlockData(ageable);
                }
            }
        }, delay);

        event.getPlayer().sendMessage("§eYou placed a §6" + def.itemName() + "§e!");
    }

    // ─────────────────────────────
    // Shift + Right Click para colocar un stack (usa stack_limit del farms.yml)
    // ─────────────────────────────
    @EventHandler
    public void onInteractPlaceStack(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || !inHand.hasItemMeta()) return;

        FarmsConfig cfg = FarmsConfig.get();
        String farmType = cfg.getFarmType(inHand);
        // Compatibilidad por nombre antiguo
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
            player.sendMessage("§cYou need an empty block above to place the farm.");
            return;
        }

        int stackLimit = Math.max(1, cfg.getStackLimit());
        int farms = Math.max(1, Math.min(inHand.getAmount(), stackLimit));

        // Colocar el bloque base configurado
        placeAt.setType(def.baseBlock());

        // Registrar farms (tipo + cantidad)
        FarmsManager.setFarm(placeAt.getLocation(), def.type(), farms);

        // Vaciar la mano (consumimos el stack completo usado)
        inHand.setAmount(0);

        // Generar cultivo maduro tras el delay configurado
        int delay = Math.max(1, cfg.getRegrowDelayTicks());
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (placeAt.getType() == def.baseBlock() && above.getType() == Material.AIR) {
                above.setType(def.cropBlock());
                if (above.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    above.setBlockData(ageable);
                }
            }
        }, delay);

        player.sendMessage("§eYou placed §f" + farms + " §6" + def.itemName() + "§e in one block!");
        event.setCancelled(true);
    }

    // ─────────────────────────────
    // Left Click para recoger la farm (sin romper vanilla)
    // ─────────────────────────────
    @EventHandler
    public void onLeftClickFarm(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (!FarmsManager.isFarm(clicked.getLocation())) return;

        event.setCancelled(true);
        breakFarmAndGiveItem(clicked, event.getPlayer());
    }

    // ─────────────────────────────
    // Romper cultivo (recompensas + regen) → solo si debajo hay una farm registrada
    // Acepta cualquier cultivo Ageable definido en farms.yml (wheat, carrots, etc.)
    // ─────────────────────────────
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // ——— Romper cultivo (cualquier Ageable)
        if (block.getBlockData() instanceof Ageable) {
            Block base = block.getRelative(0, -1, 0);
            if (!FarmsManager.isFarm(base.getLocation())) return;

            Player player = event.getPlayer();

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage("§cYour inventory is full! You cannot harvest until you make space.");
                event.setCancelled(true);
                return;
            }

            // Evitar drops vanilla
            event.setDropItems(false);

            FarmsManager.FarmData data = FarmsManager.getFarmData(base.getLocation());
            if (data == null) return;

            FarmsConfig.FarmDef def = FarmsConfig.get().getFarm(data.type());
            if (def == null) return;

            // Dar recompensa configurada (amount_per_farm * count)
            int total = Math.max(1, def.rewardAmountPerFarm() * Math.max(1, data.count()));
            ItemStack reward = new ItemStack(def.rewardType(), total);
            player.getInventory().addItem(reward);

            // Regenerar el cultivo tras el delay configurado
            int delay = Math.max(1, FarmsConfig.get().getRegrowDelayTicks());
            Block crop = block;
            Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
                if (base.getType() == def.baseBlock() && crop.getType() == Material.AIR) {
                    crop.setType(def.cropBlock());
                    if (crop.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(ageable.getMaximumAge());
                        crop.setBlockData(ageable);
                    }
                }
            }, delay);
            return;
        }

        // ——— Romper bloque base de la farm
        if (FarmsManager.isFarm(block.getLocation())) {
            event.setDropItems(false);
            event.setCancelled(true);
            breakFarmAndGiveItem(block, event.getPlayer());
        }
    }

    // ─────────────────────────────
    // Evitar que el cultivo se rompa por física cuando es parte de una farm
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

    // ─────────────────────────────
    // Lógica compartida para recoger la farm
    // Devuelve exactamente 'count' ítems del tipo correcto (apilados hasta 64).
    // Si no hay espacio suficiente, deja los sobrantes en el suelo.
    // ─────────────────────────────
    private void breakFarmAndGiveItem(Block baseBlock, Player player) {
        FarmsManager.FarmData data = FarmsManager.getFarmData(baseBlock.getLocation());
        if (data == null) return;

        // Eliminar cultivo de arriba si existe
        Block above = baseBlock.getRelative(0, 1, 0);
        if (above.getBlockData() instanceof Ageable) {
            above.setType(Material.AIR);
        }

        int remaining = Math.max(1, data.count());
        boolean droppedOnGround = false;

        while (remaining > 0) {
            int give = Math.min(64, remaining);

            // Construir ítem correcto según farms.yml
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
                leftover.values().forEach(item ->
                        player.getWorld().dropItemNaturally(player.getLocation(), item)
                );
                droppedOnGround = true;
            }

            remaining -= give;
        }

        if (droppedOnGround) {
            player.sendMessage("§eYour inventory was partially full. The remaining farms were dropped on the ground.");
        }

        // Quitar del mundo y del manager
        baseBlock.setType(Material.AIR);
        FarmsManager.removeFarm(baseBlock.getLocation());
    }
}
