package io.hnewey.landplugin;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import com.sk89q.worldedit.world.chunk.Chunk;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class LandCommands implements CommandExecutor, TabCompleter {
    private final LandPlugin plugin;
    private final LandManager land_manager;
    private final TrustedPlayerManager trusted_player_manager;
    private final LandBlockManager block_manager;
    private final List<Pattern> name_blacklist;

    LandCommands(LandPlugin plugin, LandManager land_manager, TrustedPlayerManager trusted_player_manager, LandBlockManager block_manager) {
        this.plugin = plugin;
        this.land_manager = land_manager;
        this.block_manager = block_manager;
        this.trusted_player_manager = trusted_player_manager;
        this.name_blacklist = loadNameBlacklist();
    }

    private List<Pattern> loadNameBlacklist() {
        List<Pattern> patterns = new ArrayList<>();
        for (String entry: plugin.getConfig().getStringList("land_name_blacklist")) {
            try {
                patterns.add(Pattern.compile(entry, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException ex) {
                plugin.getLogger().warning("Invalid land_name_blacklist pattern '" + entry + "': " + ex.getMessage());
            } 
        }
        return patterns;
    }

    private boolean isBlacklistedName(String name) {
        for (Pattern pattern : name_blacklist) {
            if (pattern.matcher(name).find()) {
                return true;
            }
        }
        return false;
    }

    @Override 
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        
        if (player.hasPermission("land.use")) {
            if (args.length == 0) {
                printHelp(player);
                return true;
            }

            String sub = args[0].toLowerCase(Locale.ROOT);

            switch(sub) {
                case "add": {
                    // Check if other player provided
                    if (args.length < 2) {
                        player.sendMessage("§7You must provide a §cvalid player§7 to add to your land.");
                        return true;
                    }

                    // Get player to add
                    String player_name = args[1];
                    Player other = Bukkit.getPlayer(player_name);

                    if (other == null) {
                        player.sendMessage("§7You must provide a §cvalid online player§7 to add to your land.");
                        return true;
                    } else if (other.getUniqueId() == player.getUniqueId()) {
                        player.sendMessage("§7You cannot add §cyourself§7 to your own land.");
                        return true;
                    } else if (trusted_player_manager.isTrusted(player, other)) {
                        player.sendMessage("§7The player is §calready added§7 to your land.");
                        return true;
                    }

                    trusted_player_manager.addTrusted(player, other);
                    player.sendMessage("§7The player has been §6added§7 to your land.");

                    return true;
                }
                case "remove": {
                    // Check if other player provided
                    if (args.length < 2) {
                        player.sendMessage("§7You must provide a §cvalid player§7 to remove from your land.");
                        return true;
                    }

                    // Get player to remove
                    String player_name = args[1];
                    Player other = Bukkit.getPlayer(player_name);

                    if (other == null) {
                        player.sendMessage("§7You must provide a §cvalid online player§7 to remove from your land.");
                        return true;
                    } else if (other.getUniqueId() == player.getUniqueId()) {
                        player.sendMessage("§7You cannot remove §cyourself§7 from your own land.");
                        return true;
                    } else if (!trusted_player_manager.isTrusted(player, other)) {
                        player.sendMessage("§7The player is §calready removed§7 from your land.");
                        return true;
                    }

                    trusted_player_manager.removeTrusted(player, other);
                    player.sendMessage("§7The player has been §6removed§7 from your land.");

                    return true;
                }
                case "name": {
                    // Check if the player has provided a name
                    if (args.length < 2) {
                        player.sendMessage("§7You must provide a §cname§7 for your land.");
                        return true;
                    }

                    // Check if too many args were provided
                    if (args.length > 2) {
                        player.sendMessage("§7Your land name can only be a §csingle word§7.");
                        return true;
                    }

                    // Check if the name is too long
                    if (args[1].length() > 15) {
                        player.sendMessage("§7Your land name cannot exceed §c15 characters§7.");
                        return true;
                    }

                    // Check player is in their own land
                    Location loc = player.getLocation();
                    if (loc == null) {
                        return true;
                    }

                    // Get chunk key for location
                    ChunkKey ck = LandManager.generateChunkKey(loc);

                    Land land = land_manager.getLand(ck);
                    if (land == null || !land.isOwner(player)) {
                        player.sendMessage("§7You must be in §cyour own§7 land to set the name!");
                        return true;
                    }

                    // Check name is not on blacklist
                    if (isBlacklistedName(args[1])) {
                        player.sendMessage("§7You are §cnot allowed§7 to set your land to this name!");
                        return true;
                    }

                    land.setName(args[1]);
                    player.sendMessage("§7The land's name has been set to §6" + args[1]);
                    return true;
                }
                case "list": {
                    // Get land of player
                    Set<Land> owned_land = land_manager.getLandByOwner(player);
                    if (owned_land.isEmpty()) {
                        player.sendMessage("§7You do not own §cany land§7.");
                        return true;
                    }
                    
                    player.sendMessage(plugin.getPrefix());
                    for (Land l : owned_land) {
                        Location centre = l.getCentre();
                        if (centre == null) {
                            continue; // World not loaded
                        }
                        player.sendMessage(String.format(
                                "   §8- §6%s §7X: §6%d§7, Y: §6%d§7, Z: §6%d",
                                l.getName(),
                                centre.getBlockX(),
                                centre.getBlockY(),
                                centre.getBlockZ()
                        ));
                    }

                    return true;
                }
                case "delete": {
                    // Get chunk key by location
                    ChunkKey ck = LandManager.generateChunkKey(player.getLocation());

                    // Get land
                    Land land = land_manager.getLand(ck);
                    if (land == null || !land.isOwner(player)) {
                        player.sendMessage("§7You must be inside §cyour own§7 land to delete it.");
                        return true;
                    }

                    if (land_manager.removeLand(player.getLocation())) {
                        player.sendMessage("§7The land has been §6deleted§7.");
                    }

                    return true;
                }
                case "block": {
                    if (player.hasPermission("land.block")) {
                        ItemStack block = block_manager.getBlock();
                        if (block == null) {
                            player.sendMessage("§cUnable to generate§7 land block.");
                            return true;
                        }

                        ItemStack clone = block.clone();
                        player.getInventory().addItem(clone);
                        
                        player.sendMessage("§7You have received a §6land block§7.");
                        return true;
                    } else {
                        player.sendMessage("§7You do not have §cpermission§7 for this command!");
                        return true;
                    }
                }
                default: {
                    printHelp(player);
                    return true;
                }
            }
        } else {
            player.sendMessage("§7You do not have §cpermission§7 to use this command!");
        }

        return true;
    }

    @Override 
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("add", "remove", "name", "list", "delete"));

            if (sender.hasPermission("land.block")) {
                base.add("block");
            }

            return StringUtil.copyPartialMatches(args[0], base, new ArrayList<>());
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

    private void printHelp(Player player) {
        String prefix = plugin.getPrefix();
        player.sendMessage(prefix + "§6Available commands:");

        player.sendMessage("§7/land add §b<player> §8- Trust a player to your land.");
        player.sendMessage("§7/land remove §b<player> §8- Untrust a player from your land.");
        player.sendMessage("§7/land name §6<name> §8- Set the name of your land.");
        player.sendMessage("§7/land list §8- Show all your land.");
        player.sendMessage("§7/land delete §8- Delete the land you are inside.");

        if (player.hasPermission("land.block")) {
            player.sendMessage("§7/land block §8- Get a land block for claiming land.");
        }
    }
}