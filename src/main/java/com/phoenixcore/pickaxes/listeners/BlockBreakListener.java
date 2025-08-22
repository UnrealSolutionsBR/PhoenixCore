package com.phoenixcore.pickaxes.listeners;

import com.phoenixcore.PhoenixPrisonCore;
import com.phoenixcore.pickaxes.BlockValueManager;
import com.phoenixcore.pickaxes.PickaxeManager;
import com.phoenixcore.pickaxes.SkinManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Validar que sea un pico custom
        if (!PickaxeManager.isCustomPickaxe(item)) return;

        // Bloques +1
        int blocks = PickaxeManager.getBlocksBroken(item) + 1;

        // Skin y bonus
        String skinId = PickaxeManager.getSkin(item);
        SkinManager.SkinData skinData = SkinManager.getSkin(skinId);

        // XP y nivel actuales
        double xp = PickaxeManager.getXP(item);
        int level = PickaxeManager.getLevel(item);

        // XP base desde blocks.yml
        double baseValue = BlockValueManager.getValue(event.getBlock().getType());

        // Ganancia de XP con bonus de skin
        double gain = baseValue * skinData.bonus();
        xp += gain;

        // Revisar si sube de nivel
        int needed = PhoenixPrisonCore.getInstance().getConfig().getInt("pickaxes.blocks-per-level", 100) * level;
        while (xp >= needed) {
            xp -= needed;
            level++;
            player.sendMessage("§6Your pickaxe leveled up to §e" + level + "§6!");
            needed = PhoenixPrisonCore.getInstance().getConfig().getInt("pickaxes.blocks-per-level", 100) * level;
        }

        // Actualizar el pico con datos nuevos
        ItemStack updated = PickaxeManager.createPickaxe(player, blocks, level, xp, 0, 0, skinId);
        player.getInventory().setItemInMainHand(updated);
    }
}
