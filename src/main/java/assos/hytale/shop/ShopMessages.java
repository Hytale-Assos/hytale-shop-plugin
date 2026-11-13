package assos.hytale.shop;

import assos.hytale.shop.shop.ShopService.TradeResult;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import javax.annotation.Nonnull;
import java.text.NumberFormat;
import java.util.Locale;

public final class ShopMessages {

    public static final String ERROR = "#FF5555";
    public static final String SUCCESS = "#55FF55";
    public static final String INFO = "#AAAAAA";
    public static final String HIGHLIGHT = "#FFFF55";

    private final String currencyName;

    public ShopMessages(@Nonnull String currencyName) {
        this.currencyName = currencyName;
    }

    @Nonnull
    public String getCurrencyName() {
        return currencyName;
    }

    @Nonnull
    public String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(amount) + " " + currencyName;
    }

    @Nonnull
    public static Message error(@Nonnull String text) {
        return Message.raw(text).color(ERROR);
    }

    @Nonnull
    public static Message success(@Nonnull String text) {
        return Message.raw(text).color(SUCCESS);
    }

    @Nonnull
    public static Message info(@Nonnull String text) {
        return Message.raw(text).color(INFO);
    }

    @Nonnull
    public static Message highlight(@Nonnull String text) {
        return Message.raw(text).color(HIGHLIGHT);
    }

    /**
     * Localized item name, falling back to the raw id for unknown items.
     */
    @Nonnull
    public static Message itemName(@Nonnull String itemId) {
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null || item.getTranslationKey() == null) {
            return Message.raw(itemId);
        }
        return item.getTranslationMessage();
    }

    public static boolean isKnownItem(@Nonnull String itemId) {
        return Item.getAssetMap().getAsset(itemId) != null;
    }

    @Nonnull
    public Message buyResult(@Nonnull TradeResult result, int maxQuantity) {
        if (result.succeeded()) {
            return Message.join(success("Bought " + result.quantity() + "x "),
                    itemName(result.itemId()).color(SUCCESS),
                    success(" for " + money(result.total()) + ". Balance: " + money(result.balance()) + "."));
        }
        return failure(result, maxQuantity);
    }

    @Nonnull
    public Message sellResult(@Nonnull TradeResult result, int maxQuantity) {
        if (result.succeeded()) {
            return Message.join(success("Sold " + result.quantity() + "x "),
                    itemName(result.itemId()).color(SUCCESS),
                    success(" for " + money(result.total()) + ". Balance: " + money(result.balance()) + "."));
        }
        return failure(result, maxQuantity);
    }

    @Nonnull
    private Message failure(@Nonnull TradeResult result, int maxQuantity) {
        return switch (result.status()) {
            case NOT_IN_SHOP -> error("'" + result.itemId() + "' is not sold in the shop. Use /shop list.");
            case NOT_FOR_SALE -> Message.join(error("The shop does not sell "), itemName(result.itemId()).color(ERROR),
                    error("."));
            case NOT_BOUGHT_BY_SHOP -> Message.join(error("The shop does not buy "),
                    itemName(result.itemId()).color(ERROR), error("."));
            case INVALID_QUANTITY -> error("The quantity must be at least 1.");
            case QUANTITY_TOO_LARGE -> error("You can trade at most " + maxQuantity + " items at once.");
            case INSUFFICIENT_FUNDS -> error("You need " + money(result.total()) + " for this purchase.");
            case INVENTORY_FULL -> error("Your inventory does not have room for " + result.quantity() + " items.");
            case NOT_ENOUGH_ITEMS -> Message.join(error("You do not have enough "),
                    itemName(result.itemId()).color(ERROR), error(" to sell."));
            case SUCCESS -> throw new IllegalStateException("Not a failure");
        };
    }
}
