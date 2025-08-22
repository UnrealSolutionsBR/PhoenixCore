package com.phoenixcore.commands;

import com.phoenixcore.pickaxes.SkinManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CoreTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("phoenixcore.reload")) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("phoenixcore.setskin")) {
                suggestions.add("setskin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("setskin")) {
            // Autocompletar IDs de skins desde SkinManager
            suggestions.addAll(SkinManager.getAllSkinIds());
        }

        return suggestions;
    }
}
