package io.hnewey.landplugin;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;

import java.util.*;

public class LandCommands implements CommandExecutor, TabCompleter {
    private final LandPlugin plugin;
    private final LandManager land_manager;

    LandCommands(LandPlugin plugin, LandManager land_manager) {
        this.plugin = plugin;
        this.land_manager = land_manager;
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

                return true;
            }

            String sub = args[0].toLowerCase(Locale.ROOT);

            switch(sub) {
                case "add": {
                    // Check if other player provided
                    if (args.length < 2) {
                        player.sendMessage("§4You must provide a valid player to add to your land.");
                        return true;
                    }

                    // Get land the player is in
                    Land land = land_manager.getLandByLocation(player.getLocation());
                    if (land == null || !land.isOwner(player.getUniqueId())) {
                        player.sendMessage("§4You must be inside your own land to add another player.");
                        return true;
                    }
                    
                    // Get player to add
                    String player_name = args[1];
                    Player other = Bukkit.getPlayer(player_name);

                    if (other == null) {
                        player.sendMessage("§4You must provide a valid player to add to your land.");
                        return true;
                    } else if (land.isOwner(other.getUniqueId())) {
                        player.sendMessage("§4You cannot add yourself to your own land.");
                        return true;
                    } else if (land.isTrusted(other.getUniqueId())) {
                        player.sendMessage("§4The player is already added to your land.");
                        return true;
                    }

                    land.addTrusted(other.getUniqueId());
                    player.sendMessage("§aThe player has been added to your land.");

                    return true;
                }
                case "remove": {
                    // Check if other player provided
                    if (args.length < 2) {
                        player.sendMessage("§4You must provide a valid player to remove from your land.");
                        return true;
                    }

                    // Get land the player is in
                    Land land = land_manager.getLandByLocation(player.getLocation());
                    if (land == null || !land.isOwner(player.getUniqueId())) {
                        player.sendMessage("§4You must be inside your own land to add remove player.");
                        return true;
                    }

                    // Get player to remove
                    String player_name = args[1];
                    Player other = Bukkit.getPlayer(player_name);

                    if (other == null) {
                        player.sendMessage("§4You must provide a valid player to remove from your land.");
                        return true;
                    } else if (land.isOwner(other.getUniqueId())) {
                        player.sendMessage("§4You cannot remove yourself from your own land.");
                        return true;
                    } else if (!land.isTrusted(other.getUniqueId())) {
                        player.sendMessage("§4The player is already removed from your land.");
                        return true;
                    }

                    land.removeTrusted(other.getUniqueId());
                    player.sendMessage("§aThe player has been removed from your land.");

                    return true;
                }
                case "list": {
                    // Get land of player
                    Set<Land> land = land_manager.getLandByOwner(player.getUniqueId());
                    if (land.isEmpty()) {
                        player.sendMessage("§4You do not own any land.");
                        return true;
                    }
                    
                    player.sendMessage("§2Land:");
                    int index = 1;
                    for (Land l : land) {
                        Location centre = l.getCentre();
                        player.sendMessage(String.format(
                            "§8%d. §a%s §8- §7X: §a%d§7, Y: §a%d§7, Z: §a%d",
                            index++,
                            centre.getWorld().getName(),
                            centre.getBlockX(),
                            centre.getBlockY(),
                            centre.getBlockZ()
                        ));
                    }

                    return true;
                }
                case "delete": {
                    // Get land
                    Land land = land_manager.getLandByLocation(player.getLocation());
                    if (land == null || !land.isOwner(player.getUniqueId())) {
                        player.sendMessage("§4You must be inside your own land to delete it.");
                        return true;
                    }

                    land_manager.deleteLand(land);
                    player.sendMessage("§aThe land has been deleted.");

                    return true;
                }
            }
        } else {
            player.sendMessage("§4You do not have permission to use this command!");
        }

        return true;
    }

    @Override 
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("add", "remove", "list", "delete"));
            return base;
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) { 
                case "add":
                case "remove":
                    List<String> names = new ArrayList<>();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (sender instanceof Player && online.getUniqueId().equals(((Player) sender).getUniqueId())) {
                            continue;
                        }
                        names.add(online.getName());
                    }
                    return names;
            }
        }

        return Collections.emptyList();
    }

}