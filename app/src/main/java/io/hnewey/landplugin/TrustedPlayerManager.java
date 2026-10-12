package io.hnewey.landplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TrustedPlayerManager {
    TrustedPlayerManager(LandPlugin plugin) {
        players_folder = new File(plugin.getDataFolder(), "players");
        if (!players_folder.exists()) {
            players_folder.mkdirs();
        }

        loadTrusted();
    }

    public void addTrusted(UUID owner_id, UUID other_id) {
        trusted_players.computeIfAbsent(owner_id, k -> new HashSet<>()).add(other_id);
    }

    public void removeTrusted(UUID owner_id, UUID other_id) {
        Set<UUID> trusted = trusted_players.get(owner_id);
        if (trusted == null) {
            return;
        }
        trusted.remove(other_id);
        if (trusted.isEmpty()) {
            trusted_players.remove(owner_id);
        }
    }

    public void saveTrusted() {
        File file = new File(players_folder, "players.yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Set<UUID>> entry : trusted_players.entrySet()) {
            // Save as a list of UUID strings
            List<String> trusted = entry.getValue().stream().map(UUID::toString).toList();
            config.set(entry.getKey().toString(), trusted);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            LandPlugin.getInstance().getLogger().severe("Failed to save players file " + file.getName() + ": " + e.getMessage());
        }
    }

    public boolean isTrusted(UUID owner_id, UUID player_id) {
        Set<UUID> trusted = trusted_players.get(owner_id);
        if (trusted == null) {
            return false;
        }
        return trusted.contains(player_id);
    }

    private void loadTrusted() {
        File file = new File(players_folder, "players.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Every top level key is an owner's UUID
        for (String owner_key : config.getKeys(false)) {
            try {
                UUID owner_id = UUID.fromString(owner_key);

                Set<UUID> trusted = new HashSet<>();
                for (String trusted_key : config.getStringList(owner_key)) {
                    trusted.add(UUID.fromString(trusted_key));
                }

                if (!trusted.isEmpty()) {
                    trusted_players.put(owner_id, trusted);
                }
            } catch (IllegalArgumentException e) {
                LandPlugin.getInstance().getLogger().warning("Skipping invalid UUID entry in " + file.getName() + ": " + owner_key);
            }
        }
    }

    private File players_folder;
    private Map<UUID, Set<UUID>> trusted_players = new HashMap<>();
}
