package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FarmsListener implements Listener {

    // ─────────────────────────────
    // Colocar farm normal (1x)
    // ─────────────────────────────
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (block.getType() != Material.HAY_BLOCK) return;

        Block above = block.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) {
            player.sendMessage("§cYou need an empty block above to place the farm.");
            return;
        }

        // Guardar 1 farm en el manager
        FarmsManager.setFarm(block.getLocation(), 1);

        // Spawn de trigo maduro en 5s
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (block.getType() == Material.HAY_BLOCK && above.getType() == Material.AIR) {
                above.setType(Material.WHEAT);
                if (above.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    above.setBlockData(ageable);
                }
            }
        }, 100L);

        player.sendMessage("§eYou placed a §6Wheat Farm§e!");
    }

    // ─────────────────────────────
    // Shift + Right Click para colocar un stack
    // ─────────────────────────────
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getPlayer().isSneaking()) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getType() != Material.HAY_BLOCK || !inHand.hasItemMeta()) return;

        ItemMeta meta = inHand.getItemMeta();
        if (meta == null || !"§eWheat Farm".equals(meta.getDisplayName())) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Block placeAt = clicked.getRelative(event.getBlockFace());
        if (placeAt.getType() != Material.AIR) return;

        int farms = Math.max(1, Math.min(inHand.getAmount(), 64));

        // Colocar hay bale y guardar farms en el manager
        placeAt.setType(Material.HAY_BLOCK);
        FarmsManager.setFarm(placeAt.getLocation(), farms);

        // Vaciar la mano
        inHand.setAmount(0);

        // Generar trigo maduro en 5s
        Block above = placeAt.getRelative(0, 1, 0);
        Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
            if (placeAt.getType() == Material.HAY_BLOCK && above.getType() == Material.AIR) {
                above.setType(Material.WHEAT);
                if (above.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    above.setBlockData(ageable);
                }
            }
        }, 100L);

        player.sendMessage("§eYou placed §f" + farms + " §6Wheat Farms§e in one block!");
        event.setCancelled(true);
    }

    // ─────────────────────────────
    // Romper trigo (rewards + regen)
    // ─────────────────────────────
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // ——— Romper trigo
        if (block.getType() == Material.WHEAT) {
            Player player = event.getPlayer();
            Block hayBlock = block.getRelative(0, -1, 0);

            if (hayBlock.getType() == Material.HAY_BLOCK) {
                int farms = FarmsManager.getFarm(hayBlock.getLocation());

                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§cYour inventory is full! You cannot harvest until you make space.");
                    event.setCancelled(true);
                    return;
                }

                // Evitar drops vanilla
                event.setDropItems(false);

                // Dar recompensa proporcional
                ItemStack reward = new ItemStack(Material.DIAMOND, Math.max(1, farms));
                ItemMeta meta = reward.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6Wheat x" + farms);
                    reward.setItemMeta(meta);
                }
                player.getInventory().addItem(reward);

                // Regenerar trigo en 5s si el hay bale sigue
                Block wheatBlock = block;
                Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
                    if (hayBlock.getType() == Material.HAY_BLOCK && wheatBlock.getType() == Material.AIR) {
                        wheatBlock.setType(Material.WHEAT);
                        if (wheatBlock.getBlockData() instanceof Ageable ageable) {
                            ageable.setAge(ageable.getMaximumAge());
                            wheatBlock.setBlockData(ageable);
                        }
                    }
                }, 100L);
            }
            return;
        }

        // ——— Romper hay bale (farm)
        if (block.getType() == Material.HAY_BLOCK) {
            Player player = event.getPlayer();
            int farms = FarmsManager.getFarm(block.getLocation());

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage("§cYour inventory is full! You cannot break this farm until you make space.");
                event.setCancelled(true);
                return;
            }

            // Evitar drops vanilla
            event.setDropItems(false);

            // Eliminar trigo de arriba si existe
            Block above = block.getRelative(0, 1, 0);
            if (above.getType() == Material.WHEAT) {
                above.setType(Material.AIR);
            }

            // Dar Wheat Farm (1 ítem con lore, mostrando cuántas contenía)
            ItemStack farmItem = new ItemStack(Material.HAY_BLOCK, 1);
            ItemMeta meta = farmItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eWheat Farm");
                meta.setLore(List.of(
                        "§7Place this block on the ground",
                        "§7to create a wheat farm",
                        "§8Contains: §f" + farms
                ));
                farmItem.setItemMeta(meta);
            }
            player.getInventory().addItem(farmItem);

            // Eliminar del manager
            FarmsManager.removeFarm(block.getLocation());
        }
    }

    // ─────────────────────────────
    // Evitar que el trigo se rompa por física
    // ─────────────────────────────
    @EventHandler
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.WHEAT) {
            Block below = block.getRelative(0, -1, 0);
            if (below.getType() == Material.HAY_BLOCK) {
                event.setCancelled(true);
            }
        }
    }
}
