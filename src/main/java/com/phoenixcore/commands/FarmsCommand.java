package com.phoenixcore.commands;

import com.phoenixcore.farms.FarmsConfig;
import com.phoenixcore.locale.LocaleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FarmsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LocaleManager.getMessage("only-player"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(LocaleManager.getMessage("farms-usage")
                    .replace("%label%", label));
            player.sendMessage(LocaleManager.getMessage("farms-available")
                    .replace("%list%", String.join(", ",
                            FarmsConfig.get().allFarms().stream().map(FarmsConfig.FarmDef::type).toList())));
            return true;
        }

        String farmType = args[0].toLowerCase();
        FarmsConfig.FarmDef def = FarmsConfig.get().getFarm(farmType);
        if (def == null) {
            player.sendMessage(LocaleManager.getMessage("farms-unknown-type")
                    .replace("%type%", farmType));
            player.sendMessage(LocaleManager.getMessage("farms-available")
                    .replace("%list%", String.join(", ",
                            FarmsConfig.get().allFarms().stream().map(FarmsConfig.FarmDef::type).toList())));
            return true;
        }

        int amount = 1; // default
        if (args.length >= 2) {
            try {
                amount = Math.min(Integer.parseInt(args[1]), FarmsConfig.get().getStackLimit());
                if (amount <= 0) amount = 1;
            } catch (NumberFormatException e) {
                player.sendMessage(LocaleManager.getMessage("farms-invalid-amount")
                        .replace("%amount%", args[1]));
            }
        }

        ItemStack farmItem = FarmsConfig.get().buildFarmItem(farmType, amount);
        if (farmItem == null) {
            player.sendMessage(LocaleManager.getMessage("farms-give-error"));
            return true;
        }

        player.getInventory().addItem(farmItem);
        player.sendMessage(LocaleManager.getMessage("farms-give-success")
                .replace("%amount%", String.valueOf(amount))
                .replace("%farm_name%", def.itemName()));
        return true;
    }
}
