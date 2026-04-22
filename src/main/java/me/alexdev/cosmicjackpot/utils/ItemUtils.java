package me.alexdev.cosmicjackpot.utils;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {
    public static ItemStack createItem(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(name);
        im.setLore(lore);
        item.setItemMeta(im);
        return item;
    }
}

