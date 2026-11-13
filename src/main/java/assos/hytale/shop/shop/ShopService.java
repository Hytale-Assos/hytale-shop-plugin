package assos.hytale.shop.shop;

import assos.hytale.shop.economy.EconomyService;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Buy and sell rules of the admin shop: the shop has unlimited stock and unlimited Hcoins.
 *
 * <p>Money and items move in an order that never creates value: on a buy the Hcoins are taken before the items
 * are given (and refunded if giving fails); on a sell the items are taken before the Hcoins are paid (and given
 * back if paying fails).</p>
 */
public final class ShopService {

    private static final Logger LOGGER = Logger.getLogger(ShopService.class.getName());

    private final ShopCatalog catalog;
    private final EconomyService economy;
    private final int maxQuantity;

    public ShopService(@Nonnull ShopCatalog catalog, @Nonnull EconomyService economy, int maxQuantity) {
        if (maxQuantity < 1) {
            throw new IllegalArgumentException("Max quantity must be at least 1");
        }
        this.catalog = catalog;
        this.economy = economy;
        this.maxQuantity = maxQuantity;
    }

    @Nonnull
    public ShopCatalog getCatalog() {
        return catalog;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    @Nonnull
    public TradeResult buy(@Nonnull UUID playerId, @Nonnull PlayerInventory inventory, @Nonnull String itemId,
            int quantity) throws IOException {
        Optional<ShopEntry> found = catalog.find(itemId);
        if (found.isEmpty()) {
            return TradeResult.failure(TradeStatus.NOT_IN_SHOP, itemId, quantity);
        }
        ShopEntry entry = found.get();
        if (!entry.canBuy()) {
            return TradeResult.failure(TradeStatus.NOT_FOR_SALE, entry.itemId(), quantity);
        }
        TradeStatus quantityStatus = checkQuantity(quantity);
        if (quantityStatus != null) {
            return TradeResult.failure(quantityStatus, entry.itemId(), quantity);
        }

        long total = Math.multiplyExact(entry.buyPrice(), (long) quantity);
        if (economy.getBalance(playerId) < total) {
            return TradeResult.failure(TradeStatus.INSUFFICIENT_FUNDS, entry.itemId(), quantity, total);
        }
        if (!inventory.canAdd(entry.itemId(), quantity)) {
            return TradeResult.failure(TradeStatus.INVENTORY_FULL, entry.itemId(), quantity, total);
        }
        if (!economy.withdraw(playerId, total)) {
            return TradeResult.failure(TradeStatus.INSUFFICIENT_FUNDS, entry.itemId(), quantity, total);
        }
        if (!inventory.add(entry.itemId(), quantity)) {
            economy.deposit(playerId, total);
            return TradeResult.failure(TradeStatus.INVENTORY_FULL, entry.itemId(), quantity, total);
        }
        return TradeResult.success(entry.itemId(), quantity, total, economy.getBalance(playerId));
    }

    @Nonnull
    public TradeResult sell(@Nonnull UUID playerId, @Nonnull PlayerInventory inventory, @Nonnull String itemId,
            int quantity) throws IOException {
        Optional<ShopEntry> found = catalog.find(itemId);
        if (found.isEmpty()) {
            return TradeResult.failure(TradeStatus.NOT_IN_SHOP, itemId, quantity);
        }
        ShopEntry entry = found.get();
        if (!entry.canSell()) {
            return TradeResult.failure(TradeStatus.NOT_BOUGHT_BY_SHOP, entry.itemId(), quantity);
        }
        TradeStatus quantityStatus = checkQuantity(quantity);
        if (quantityStatus != null) {
            return TradeResult.failure(quantityStatus, entry.itemId(), quantity);
        }

        long total = Math.multiplyExact(entry.sellPrice(), (long) quantity);
        if (inventory.count(entry.itemId()) < quantity || !inventory.remove(entry.itemId(), quantity)) {
            return TradeResult.failure(TradeStatus.NOT_ENOUGH_ITEMS, entry.itemId(), quantity, total);
        }
        try {
            economy.deposit(playerId, total);
        } catch (IOException | RuntimeException exception) {
            if (!inventory.add(entry.itemId(), quantity)) {
                LOGGER.log(Level.SEVERE, "Could not give back " + quantity + " " + entry.itemId() + " to "
                        + playerId + " after a failed sale");
            }
            throw exception;
        }
        return TradeResult.success(entry.itemId(), quantity, total, economy.getBalance(playerId));
    }

    /**
     * Sells every unit of the item the player carries, capped at the max transaction quantity.
     */
    @Nonnull
    public TradeResult sellAll(@Nonnull UUID playerId, @Nonnull PlayerInventory inventory, @Nonnull String itemId)
            throws IOException {
        Optional<ShopEntry> found = catalog.find(itemId);
        if (found.isEmpty()) {
            return TradeResult.failure(TradeStatus.NOT_IN_SHOP, itemId, 0);
        }
        ShopEntry entry = found.get();
        if (!entry.canSell()) {
            return TradeResult.failure(TradeStatus.NOT_BOUGHT_BY_SHOP, entry.itemId(), 0);
        }
        int owned = inventory.count(entry.itemId());
        if (owned == 0) {
            return TradeResult.failure(TradeStatus.NOT_ENOUGH_ITEMS, entry.itemId(), 0);
        }
        return sell(playerId, inventory, entry.itemId(), Math.min(owned, maxQuantity));
    }

    private TradeStatus checkQuantity(int quantity) {
        if (quantity < 1) {
            return TradeStatus.INVALID_QUANTITY;
        }
        if (quantity > maxQuantity) {
            return TradeStatus.QUANTITY_TOO_LARGE;
        }
        return null;
    }

    public enum TradeStatus {
        SUCCESS,
        NOT_IN_SHOP,
        NOT_FOR_SALE,
        NOT_BOUGHT_BY_SHOP,
        INVALID_QUANTITY,
        QUANTITY_TOO_LARGE,
        INSUFFICIENT_FUNDS,
        INVENTORY_FULL,
        NOT_ENOUGH_ITEMS
    }

    /**
     * @param total   Hcoins moved, or that would have moved for a failure where it is known (otherwise 0)
     * @param balance balance after a success, otherwise -1
     */
    public record TradeResult(@Nonnull TradeStatus status, @Nonnull String itemId, int quantity, long total,
            long balance) {

        static TradeResult success(String itemId, int quantity, long total, long balance) {
            return new TradeResult(TradeStatus.SUCCESS, itemId, quantity, total, balance);
        }

        static TradeResult failure(TradeStatus status, String itemId, int quantity) {
            return failure(status, itemId, quantity, 0);
        }

        static TradeResult failure(TradeStatus status, String itemId, int quantity, long total) {
            return new TradeResult(status, itemId, quantity, total, -1);
        }

        public boolean succeeded() {
            return status == TradeStatus.SUCCESS;
        }
    }
}
