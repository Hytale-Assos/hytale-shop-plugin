package assos.hytale.shop.shop;

import javax.annotation.Nonnull;

/**
 * Default prices of the blocks added automatically to the shop. Admins override any of them with
 * {@code /shopadmin set}.
 */
public final class AutoPricing {

    private AutoPricing() {
    }

    /**
     * Base price of the category, doubled for each quality step above common: junk and common x1, uncommon x2,
     * rare x4, epic x8, legendary x16.
     *
     * @param qualityValue the game's quality value (0 junk, 1 common ... 5 legendary)
     */
    public static long buyPrice(@Nonnull ShopCategory category, int qualityValue) {
        int steps = Math.min(Math.max(qualityValue - 1, 0), 4);
        return category.getBasePrice() << steps;
    }

    /**
     * A share of the buy price, at least 1 so selling is always possible, and never above the buy price.
     */
    public static long sellPrice(long buyPrice, int sellPercent) {
        int percent = Math.min(Math.max(sellPercent, 0), 100);
        if (percent == 0) {
            return 0;
        }
        return Math.min(buyPrice, Math.max(1, buyPrice * percent / 100));
    }
}
