package me.economybridge;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;
import org.quickshop.api.QuickShopAPI;
import org.quickshop.api.shop.Shop;
import java.util.HashMap;
import java.util.Map;

/**
 * ShopSync - Handles QuickShop-Hikari synchronization
 * Updates all shop prices based on Skript economy variables
 */
public class ShopSync {

    private final Plugin plugin;
    private final SkriptBridge skriptBridge;
    private final QuickShopAPI quickShopAPI;

    // Material to Skript variable mapping
    private static final Map<Material, String> MATERIAL_MAPPING = new HashMap<>();

    static {
        MATERIAL_MAPPING.put(Material.DIAMOND, "diamond");
        MATERIAL_MAPPING.put(Material.IRON_INGOT, "iron");
        MATERIAL_MAPPING.put(Material.GOLD_INGOT, "gold");
        MATERIAL_MAPPING.put(Material.EMERALD, "emerald");
        MATERIAL_MAPPING.put(Material.COPPER_INGOT, "copper");
        MATERIAL_MAPPING.put(Material.REDSTONE, "redstone");
        MATERIAL_MAPPING.put(Material.LAPIS_LAZULI, "lapis");
        MATERIAL_MAPPING.put(Material.COAL, "coal");
        MATERIAL_MAPPING.put(Material.QUARTZ, "quartz");
    }

    public ShopSync(Plugin plugin, SkriptBridge skriptBridge) {
        this.plugin = plugin;
        this.skriptBridge = skriptBridge;
        this.quickShopAPI = QuickShopAPI.getInstance();
    }

    /**
     * Updates all QuickShop prices based on Skript variables
     * @return Number of shops updated
     */
    public int updateAll() {
        if (quickShopAPI == null) {
            plugin.getLogger().severe("QuickShop API is not available!");
            return 0;
        }

        int updatedCount = 0;

        try {
            // Get all loaded shops from QuickShop
            var allShops = quickShopAPI.getShopManager().getAllShops();

            if (allShops == null || allShops.isEmpty()) {
                plugin.getLogger().info("No shops found to update.");
                return 0;
            }

            for (Shop shop : allShops) {
                if (updateShop(shop)) {
                    updatedCount++;
                }
            }

            plugin.getLogger().info("Successfully updated " + updatedCount + " shops.");
            return updatedCount;

        } catch (Exception e) {
            plugin.getLogger().severe("Error during shop synchronization: " + e.getMessage());
            e.printStackTrace();
            return updatedCount;
        }
    }

    /**
     * Updates a single shop's price
     * @param shop The shop to update
     * @return true if updated, false otherwise
     */
    private boolean updateShop(Shop shop) {
        try {
            // Get shop item material
            Material itemMaterial = shop.getItem().getType();

            // Check if material is in our mapping
            if (!MATERIAL_MAPPING.containsKey(itemMaterial)) {
                return false; // Skip unmapped materials
            }

            // Get material name for lookup
            String materialName = MATERIAL_MAPPING.get(itemMaterial);

            // Get price from Skript variables
            double price = skriptBridge.getPriceForMaterial(materialName);

            // Skip if price is 0 (variable not found)
            if (price <= 0) {
                plugin.getLogger().warning("Price for material " + itemMaterial + " is " + price + ", skipping update.");
                return false;
            }

            // Update selling price
            shop.setShopPrice(price);

            // Update shop sign
            shop.update();

            // Log update
            if (shop.getLocation() != null) {
                plugin.getLogger().fine("Updated shop at " + shop.getLocation() + ": " + itemMaterial + " -> " + price);
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Error updating shop: " + e.getMessage());
            return false;
        }
    }
}