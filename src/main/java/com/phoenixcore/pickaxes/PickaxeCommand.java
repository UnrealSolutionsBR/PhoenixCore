package com.phoenixcore.pickaxes;

import com.phoenixcore.PhoenixPrisonCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PickaxeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede usarse en el juego.");
            return true;
        }

        // Obtener skin por defecto desde config.yml
        String defaultSkin = PhoenixPrisonCore.getInstance()
                .getConfig()
                .getString("pickaxes.default-skin", "wooden");

        // Crear y dar el pico inicial
        player.getInventory().addItem(PickaxeManager.createPickaxe(
                player,
                0,      // blocksBroken
                1,      // level
                0.0,    // xp inicial
                0,      // fortune
                0,      // explosive
                defaultSkin // skinId desde config.yml
        ));

        player.sendMessage("§a¡Se te ha entregado tu pico inicial!");
        return true;
    }
}
