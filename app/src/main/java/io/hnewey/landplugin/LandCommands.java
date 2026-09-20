package io.hnewey.landplugin;

import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
// import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;;

public class LandCommands implements CommandExecutor {
    private final LandPlugin plugin;

    LandCommands(LandPlugin plugin) {
        this.plugin = plugin;
    }

    @Override 
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (sender instanceof Player) ? (Player) sender : null;

        if (player.hasPermission("land.use")) {
            if (args.length == 0) {
                String prefix = plugin.getPrefix();
                player.sendMessage(prefix + "§aAvailable commands:");

                player.sendMessage("§a/land add §2<player> §8- §7Trust a player in your land.");
                player.sendMessage("§a/land remove §2<player> §8- §7Untrust a player in your land.");
                player.sendMessage("§a/land list §8- §7Show all your land.");
                player.sendMessage("§a/land delete §8- §7Delete the land you are inside.");
            }
        } else {
            player.sendMessage(plugin.getPrefix() + "§cYou do not have permission to use this command!");
        }

        return true;
    }

}