package com.phoenixcore.farms;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FarmsListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        if (block.getType() == Material.HAY_BLOCK) {
            Block above = block.getRelative(0, 1, 0);

            if (above.getType() == Material.AIR) {
                // Colocar trigo un tick después para evitar problemas de física
                Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
                    above.setType(Material.WHEAT);

                    if (above.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(ageable.getMaximumAge()); // crecer al máximo
                        above.setBlockData(ageable);
                    }
                }, 1L);

                player.sendMessage("§eYou placed a §6Wheat Farm§e!");
            } else {
                player.sendMessage("§cYou need an empty block above to place the farm.");
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() == Material.WHEAT) {
            Player player = event.getPlayer();

            // Crear el item custom "Wheat x1"
            ItemStack reward = new ItemStack(Material.DIAMOND, 1);
            ItemMeta meta = reward.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Wheat x1");
                reward.setItemMeta(meta);
            }

            // Intentar agregar al inventario
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(reward);
            } else {
                // Inventario lleno → soltar en la posición del jugador
                player.getWorld().dropItem(player.getLocation(), reward);
                player.sendMessage("§cYour inventory is full! The item was dropped at your feet.");
            }

            // Evitar drop vanilla
            event.setDropItems(false);

            // Regenerar el trigo en 5 segundos
            Block wheatBlock = block;
            Bukkit.getScheduler().runTaskLater(PhoenixPrisonCore.getInstance(), () -> {
                wheatBlock.setType(Material.WHEAT);
                if (wheatBlock.getBlockData() instanceof Ageable ageable) {
                    ageable.setAge(ageable.getMaximumAge());
                    wheatBlock.setBlockData(ageable);
                }
            }, 100L); // 100 ticks = 5 segundos
        }
    }
}
//