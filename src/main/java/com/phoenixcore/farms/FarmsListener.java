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
            event.getPlayer().sendMessage("§cYou need an empty block above to place the farm.");
            return;
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
            player.sendMessage("§cYou need an empty block above to place the farm.");
            return;
        }

        int stackLimit = Math.max(1, cfg.getStackLimit());
        int farms = Math.max(1, Math.min(inHand.getAmount(), stackLimit));

        placeAt.setType(def.baseBlock());
        FarmsManager.setFarm(placeAt.getLocation(), def.type(), farms);

        // consumir el stack usado
        inHand.setAmount(0);

        int delay = Math.max(1, cfg.getRegrowDelayTicks());
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (placeAt.getType() == def.baseBlock() && above.getType() == Material.AIR) {
                spawnMatureCrop(above, def);
            }
        }, delay);

        player.sendMessage("§eYou placed §f" + farms + " §6" + def.itemName() + "§e in one block!");
        event.setCancelled(true);
    }

    // ─────────────────────────────
    // Left Click:
    //  - Si clickeas el bloque base → recoger toda la farm
    //  - Si clickeas el cultivo encima (Ageable o no) con una farm debajo → cosechar (recompensa + regen)
    // ─────────────────────────────
    @EventHandler
    public void onLeftClickFarm(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        // 1) Si es el bloque base de una farm → recoger farm (misma lógica de antes)
        if (FarmsManager.isFarm(clicked.getLocation())) {
            event.setCancelled(true);
            breakFarmAndGiveItem(clicked, event.getPlayer());
            return;
        }

        // 2) Si es el cultivo encima de una farm (wheat/carrots o calabaza/melón, etc.) → cosechar
        Block below = clicked.getRelative(0, -1, 0);
        if (FarmsManager.isFarm(below.getLocation())) {
            event.setCancelled(true); // evita rotura vanilla
            harvestCrop(clicked, below, event.getPlayer());
        }
    }

    // ─────────────────────────────
    // Romper cultivo o bloque cosechable (wheat, carrots, pumpkin, melon, etc.)
    // Si debajo hay farm → dar recompensa y regenerar
    // ─────────────────────────────
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Caso A: cultivo Ageable (wheat, carrots, etc.)
        if (block.getBlockData() instanceof Ageable) {
            Block base = block.getRelative(0, -1, 0);
            if (!FarmsManager.isFarm(base.getLocation())) return;

            event.setDropItems(false);
            event.setCancelled(true); // manejamos la cosecha manualmente
            harvestCrop(block, base, event.getPlayer());
            return;
        }

        // Caso B: bloque cosechable NO Ageable (pumpkin, melon, etc.)
        Block base = block.getRelative(0, -1, 0);
        if (FarmsManager.isFarm(base.getLocation())) {
            event.setDropItems(false);
            event.setCancelled(true); // manejamos la cosecha manualmente
            harvestCrop(block, base, event.getPlayer());
            return;
        }

        // Caso C: romper el bloque base de la farm → recoger farm
        if (FarmsManager.isFarm(block.getLocation())) {
            event.setDropItems(false);
            event.setCancelled(true);
            breakFarmAndGiveItem(block, event.getPlayer());
        }
    }

    // ─────────────────────────────
    // Evitar que el cultivo Ageable se rompa por física cuando es parte de una farm
    // (No aplica a pumpkin/melon, pero no hace daño mantenerlo para Ageable)
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
    // Cosecha: dar recompensa y regenerar
    // 'crop' es el bloque del cultivo o del bloque cosechable (encima del base)
    // 'base' es el bloque base registrado en FarmsManager
    // ─────────────────────────────
    private void harvestCrop(Block crop, Block base, Player player) {
        FarmsManager.FarmData data = FarmsManager.getFarmData(base.getLocation());
        if (data == null) return;

        FarmsConfig.FarmDef def = FarmsConfig.get().getFarm(data.type());
        if (def == null) return;

        // Validar que el bloque que se intenta cosechar coincide con el configurado
        // (para evitar cosechar cualquier cosa encima del base).
        // Nota: Para Ageable, el tipo del bloque es el propio cultivo; para pumpkin/melon también.
        if (crop.getType() != def.cropBlock() && !(crop.getBlockData() instanceof Ageable)) {
            // Si no coincide el tipo y además no es Ageable (que igual aceptamos), no cosechar.
            return;
        }

        // Espacio de inventario
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cYour inventory is full! You cannot harvest until you make space.");
            return;
        }

        // Eliminar el cultivo actual
        crop.setType(Material.AIR);

        // Recompensa total: amount_per_farm * count
        int total = Math.max(1, def.rewardAmountPerFarm() * Math.max(1, data.count()));
        ItemStack reward = new ItemStack(def.rewardType(), total);
        player.getInventory().addItem(reward);

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
    // Para Ageable lo maxeamos; para pumpkin/melon simplemente colocamos el bloque
    // ─────────────────────────────
    private void spawnMatureCrop(Block where, FarmsConfig.FarmDef def) {
        where.setType(def.cropBlock());
        if (where.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            where.setBlockData(ageable);
        }
    }

    // ─────────────────────────────
    // Recoger la farm completa (devuelve exactamente 'count' ítems del tipo correcto)
    // ─────────────────────────────
    private void breakFarmAndGiveItem(Block baseBlock, Player player) {
        FarmsManager.FarmData data = FarmsManager.getFarmData(baseBlock.getLocation());
        if (data == null) return;

        // Quitar cultivo de arriba si existe
        Block above = baseBlock.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            above.setType(Material.AIR);
        }

        int remaining = Math.max(1, data.count());
        boolean droppedOnGround = false;

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
