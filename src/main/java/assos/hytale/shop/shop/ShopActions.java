package assos.hytale.shop.shop;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.economy.EconomyService;
import assos.hytale.shop.shop.ShopService.TradeResult;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Runs a trade for an in-game player and turns the outcome into a chat or UI message. Shared by the commands
 * and the shop page; must be called on the player's world thread.
 */
public final class ShopActions {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ShopService shopService;
    private final EconomyService economy;
    private final ShopMessages messages;

    public ShopActions(@Nonnull ShopService shopService, @Nonnull EconomyService economy,
            @Nonnull ShopMessages messages) {
        this.shopService = shopService;
        this.economy = economy;
        this.messages = messages;
    }

    @Nonnull
    public ShopService getShopService() {
        return shopService;
    }

    @Nonnull
    public EconomyService getEconomy() {
        return economy;
    }

    @Nonnull
    public ShopMessages getMessages() {
        return messages;
    }

    @Nonnull
    public Message buy(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
            @Nonnull String itemId, int quantity) {
        return run(playerRef, store, ref, "buy", (inventory) -> messages.buyResult(
                shopService.buy(playerRef.getUuid(), inventory, itemId, quantity), shopService.getMaxQuantity()));
    }

    @Nonnull
    public Message sell(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull String itemId, int quantity) {
        return run(playerRef, store, ref, "sell", (inventory) -> messages.sellResult(
                shopService.sell(playerRef.getUuid(), inventory, itemId, quantity), shopService.getMaxQuantity()));
    }

    @Nonnull
    public Message sellAll(@Nonnull PlayerRef playerRef, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull String itemId) {
        return run(playerRef, store, ref, "sell", (inventory) -> {
            TradeResult result = shopService.sellAll(playerRef.getUuid(), inventory, itemId);
            return messages.sellResult(result, shopService.getMaxQuantity());
        });
    }

    /**
     * Records the player's current name on their account, for {@code /hcointop}.
     */
    public void touch(@Nonnull PlayerRef playerRef) {
        try {
            economy.touch(playerRef.getUuid(), playerRef.getUsername());
        } catch (IOException exception) {
            LOGGER.at(Level.WARNING).withCause(exception)
                    .log("Could not save the account of %s", playerRef.getUsername());
        }
    }

    private Message run(PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref, String action,
            Trade trade) {
        PlayerInventory inventory = HytalePlayerInventory.of(store, ref);
        if (inventory == null) {
            return ShopMessages.error("Your inventory is not available right now.");
        }
        touch(playerRef);
        try {
            return trade.run(inventory);
        } catch (IOException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to save a %s transaction for %s", action, playerRef.getUsername());
            return ShopMessages.error("The transaction could not be saved. Check the server logs.");
        } catch (IllegalArgumentException exception) {
            return ShopMessages.error(exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface Trade {
        Message run(PlayerInventory inventory) throws IOException;
    }
}
