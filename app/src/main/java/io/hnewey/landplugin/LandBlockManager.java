package io.hnewey.landplugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LandBlockManager {
    private ItemStack block;

    public LandBlockManager() {
        loadBlock();
    }

    private void loadBlock() {
        // Get material
        Material mat = Material.matchMaterial("SPONGE");
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        meta.setDisplayName(ChatColor.GREEN + "Land Block");

        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Place block to claim 32x32 land");

        meta.setLore(lore);
        item.setItemMeta(meta);

        block = item;
    }

    public ItemStack getBlock() {
        if (block == null) {
            return null;
        }
        return block.clone();
    }
}
