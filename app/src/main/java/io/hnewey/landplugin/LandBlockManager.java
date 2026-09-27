package io.hnewey.landplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class LandBlockManager {
    private final LandPlugin plugin;

    private ItemStack block;
    private NamespacedKey key;

    public LandBlockManager(LandPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(this.plugin, "land_block");

        loadBlock();
    }

    private void loadBlock() {
        // Get material
        Material mat = Material.SPONGE;
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        meta.setDisplayName(ChatColor.GREEN + "Land Block");

        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Place block to claim 32x32 land");

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(this.key, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);

        block = item;
    }

    public ItemStack getBlock() {
        return block.clone();
    }

    public boolean isLandBlock(ItemStack block) {
        ItemMeta meta = block.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        if (!meta.getPersistentDataContainer().has(this.key)) {
            return false;
        }
        
        return meta.getPersistentDataContainer().get(this.key, PersistentDataType.BOOLEAN);
    }
}
