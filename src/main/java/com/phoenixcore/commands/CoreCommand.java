package com.phoenixcore.commands;

import com.phoenixcore.PhoenixPrisonCore;
import com.phoenixcore.locale.LocaleManager;
import com.phoenixcore.pickaxes.BlockValueManager;
import com.phoenixcore.pickaxes.PickaxeManager;
import com.phoenixcore.pickaxes.SkinManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CoreCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // ───────────────
        //  /phoenixcore   → Info del plugin
        // ───────────────
        if (args.length == 0) {
            List<String> infoLines = LocaleManager.getMessageList("core-info");

            for (String line : infoLines) {
                line = line
                        .replace("%version%", PhoenixPrisonCore.getInstance().getDescription().getVersion())
                        .replace("%author%", String.join(", ", PhoenixPrisonCore.getInstance().getDescription().getAuthors()))
                        .replace("%website%", PhoenixPrisonCore.getInstance().getDescription().getWebsite());
                sender.sendMessage(line);
            }
            return true;
        }

        // ───────────────
        //  /phoenixcore reload
        // ───────────────
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("phoenixcore.reload")) {
                sender.sendMessage(LocaleManager.getMessage("no-permission"));
                return true;
            }

            PhoenixPrisonCore.getInstance().reloadConfig();
            LocaleManager.loadLocale();
            SkinManager.loadSkins();
            BlockValueManager.loadValues();

            sender.sendMessage(LocaleManager.getMessage("reload"));
            return true;
        }

        // ───────────────
        //  /phoenixcore setskin <skin>
        // ───────────────
        if (args[0].equalsIgnoreCase("setskin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(LocaleManager.getMessage("only-player"));
                return true;
            }

            if (!player.hasPermission("phoenixcore.setskin")) {
                player.sendMessage(LocaleManager.getMessage("no-permission"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(LocaleManager.getMessage("setskin-usage")
                        .replace("%label%", label));
                player.sendMessage(LocaleManager.getMessage("setskin-available")
                        .replace("%list%", String.join(", ", SkinManager.getAllSkinIds())));
                return true;
            }

            String skinId = args[1].toLowerCase();

            // Validar existencia de la skin
            if (!SkinManager.getAllSkinIds().contains(skinId)) {
                player.sendMessage(LocaleManager.getMessage("setskin-not-found")
                        .replace("%skin%", skinId));
                player.sendMessage(LocaleManager.getMessage("setskin-available")
                        .replace("%list%", String.join(", ", SkinManager.getAllSkinIds())));
                return true;
            }

            // Validar que tenga un pico custom en la mano
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (!PickaxeManager.isCustomPickaxe(inHand)) {
                player.sendMessage(LocaleManager.getMessage("setskin-not-pickaxe"));
                return true;
            }

            // Recuperar stats actuales del pico
            int blocks = PickaxeManager.getBlocksBroken(inHand);
            int level = PickaxeManager.getLevel(inHand);
            double xp = PickaxeManager.getXP(inHand);
            int fortune = 0;   // reservado para futuro
            int explosive = 0; // reservado para futuro

            // Regenerar el pico con la nueva skin
            ItemStack updated = PickaxeManager.createPickaxe(player, blocks, level, xp, fortune, explosive, skinId);
            player.getInventory().setItemInMainHand(updated);

            player.sendMessage(LocaleManager.getMessage("setskin-success")
                    .replace("%skin_display%", SkinManager.getSkin(skinId).display()));
            return true;
        }

        // ───────────────
        //  Uso por defecto
        // ───────────────
        sender.sendMessage(LocaleManager.getMessage("core-usage")
                .replace("%label%", label));
        return true;
    }
}
