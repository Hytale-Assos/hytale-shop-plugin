package assos.hytale.shop.shop;

import javax.annotation.Nonnull;

/**
 * One item of the admin shop. A price of {@code 0} disables that direction: {@code buyPrice == 0} means players
 * cannot buy it, {@code sellPrice == 0} means the shop does not buy it back.
 *
 * @param buyPrice  Hcoins a player pays per unit
 * @param sellPrice Hcoins a player receives per unit
 */
public record ShopEntry(@Nonnull String itemId, long buyPrice, long sellPrice) {

    public ShopEntry {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id is required");
        }
        if (buyPrice < 0 || sellPrice < 0) {
            throw new IllegalArgumentException("Prices cannot be negative");
        }
        if (buyPrice == 0 && sellPrice == 0) {
            throw new IllegalArgumentException("At least one of the buy and sell prices must be set");
        }
    }

    public boolean canBuy() {
        return buyPrice > 0;
    }

    public boolean canSell() {
        return sellPrice > 0;
    }
}
