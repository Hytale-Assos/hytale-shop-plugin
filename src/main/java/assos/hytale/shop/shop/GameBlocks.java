package assos.hytale.shop.shop;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the blocks of the loaded game assets. Must be called after the assets are loaded (plugin start).
 */
public final class GameBlocks {

    /**
     * Developer, technical, template, tool and debug items have a quality value of 8 or more.
     */
    private static final int MAX_PLAYER_QUALITY = 5;

    private GameBlocks() {
    }

    /**
     * Every placeable block a player could own: block items in a block or furniture category, excluding
     * internal state variants and developer qualities.
     */
    @Nonnull
    public static List<ShopEntry> autoEntries(int sellPercent) {
        List<ShopEntry> entries = new ArrayList<>();
        for (Item item : Item.getAssetMap().getAssetMap().values()) {
            if (item == null || !isPlayerBlock(item)) {
                continue;
            }
            ShopCategory category = ShopCategory.fromGameCategories(item.getCategories());
            long buyPrice = AutoPricing.buyPrice(category, qualityValue(item));
            long sellPrice = AutoPricing.sellPrice(buyPrice, sellPercent);
            entries.add(new ShopEntry(item.getId(), buyPrice, sellPrice));
        }
        return entries;
    }

    @Nonnull
    public static ShopCategory categoryOf(@Nonnull String itemId) {
        Item item = Item.getAssetMap().getAsset(itemId);
        return item == null ? ShopCategory.OTHER : ShopCategory.fromGameCategories(item.getCategories());
    }

    private static boolean isPlayerBlock(Item item) {
        String id = item.getId();
        if (id == null || id.isBlank() || id.startsWith("*") || !item.hasBlockType() || item.isState()) {
            return false;
        }
        if (qualityValue(item) > MAX_PLAYER_QUALITY) {
            return false;
        }
        return ShopCategory.fromGameCategories(item.getCategories()) != ShopCategory.OTHER;
    }

    private static int qualityValue(Item item) {
        ItemQuality quality = ItemQuality.getAssetMap().getAsset(item.getQualityIndex());
        return quality == null ? 1 : quality.getQualityValue();
    }
}
