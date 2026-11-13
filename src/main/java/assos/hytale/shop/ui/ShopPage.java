package assos.hytale.shop.ui;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.shop.HytalePlayerInventory;
import assos.hytale.shop.shop.ShopActions;
import assos.hytale.shop.shop.ShopEntry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The shop window opened by {@code /shop}: one row per item with Buy, Sell and Sell all buttons, and a shared
 * quantity field. Every action rebuilds the page so balance and owned counts stay accurate.
 */
public final class ShopPage extends InteractiveCustomUIPage<ShopPage.PageData> {

    private static final String PAGE_LAYOUT = "Pages/HytaleShop/ShopPage.ui";
    private static final String ROW_LAYOUT = "Pages/HytaleShop/ShopItemRow.ui";
    private static final String ACTION_BUY = "Buy";
    private static final String ACTION_SELL = "Sell";
    private static final String ACTION_SELL_ALL = "SellAll";

    private final ShopActions actions;
    private Message status = Message.raw("");
    private String quantity = "1";

    public ShopPage(@Nonnull PlayerRef playerRef, @Nonnull ShopActions actions) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.actions = actions;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        ShopMessages messages = actions.getMessages();
        builder.append(PAGE_LAYOUT);
        builder.set("#ShopTitle.Text", messages.getCurrencyName() + " Shop");
        builder.set("#Balance.Text",
                "Balance: " + messages.money(actions.getEconomy().getBalance(playerRef.getUuid())));
        builder.set("#QuantityInput.Value", quantity);
        builder.set("#Status.TextSpans", status);

        List<ShopEntry> entries = actions.getShopService().getCatalog().list().stream()
                .filter(entry -> ShopMessages.isKnownItem(entry.itemId()))
                .toList();
        if (entries.isEmpty()) {
            builder.set("#Status.TextSpans", ShopMessages.error("The shop is empty."));
            return;
        }

        HytalePlayerInventory inventory = HytalePlayerInventory.of(store, ref);
        for (int index = 0; index < entries.size(); index++) {
            ShopEntry entry = entries.get(index);
            String row = "#ItemList[" + index + "]";
            builder.append("#ItemList", ROW_LAYOUT);
            builder.set(row + " #Icon.ItemId", entry.itemId());
            builder.set(row + " #Name.TextSpans", ShopMessages.itemName(entry.itemId()));
            builder.set(row + " #Prices.Text", "Buy: " + (entry.canBuy() ? messages.money(entry.buyPrice()) : "-")
                    + "   Sell: " + (entry.canSell() ? messages.money(entry.sellPrice()) : "-"));
            int owned = inventory == null ? 0 : inventory.count(entry.itemId());
            builder.set(row + " #Owned.Text", "In inventory: " + owned);
            builder.set(row + " #BuyButton.Disabled", !entry.canBuy());
            builder.set(row + " #SellButton.Disabled", !entry.canSell() || owned == 0);
            builder.set(row + " #SellAllButton.Disabled", !entry.canSell() || owned == 0);

            bind(eventBuilder, row + " #BuyButton", ACTION_BUY, entry.itemId());
            bind(eventBuilder, row + " #SellButton", ACTION_SELL, entry.itemId());
            bind(eventBuilder, row + " #SellAllButton", ACTION_SELL_ALL, entry.itemId());
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        if (data.action == null || data.item == null) {
            return;
        }
        if (data.quantity != null) {
            quantity = data.quantity.trim();
        }

        status = switch (data.action) {
            case ACTION_BUY -> parseQuantity() > 0
                    ? actions.buy(playerRef, store, ref, data.item, parseQuantity())
                    : invalidQuantity();
            case ACTION_SELL -> parseQuantity() > 0
                    ? actions.sell(playerRef, store, ref, data.item, parseQuantity())
                    : invalidQuantity();
            case ACTION_SELL_ALL -> actions.sellAll(playerRef, store, ref, data.item);
            default -> status;
        };
        refresh(ref, store);
    }

    private void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder builder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        build(ref, builder, eventBuilder, store);
        sendUpdate(builder, eventBuilder, true);
    }

    private int parseQuantity() {
        try {
            return Integer.parseInt(quantity);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private Message invalidQuantity() {
        quantity = "1";
        return ShopMessages.error("Enter a whole number between 1 and "
                + actions.getShopService().getMaxQuantity() + " in the quantity field.");
    }

    private static void bind(UIEventBuilder eventBuilder, String selector, String action, String itemId) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector,
                EventData.of("Action", action)
                        .append("Item", itemId)
                        .append("@Quantity", "#QuantityInput.Value"),
                false);
    }

    public static final class PageData {

        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, info) -> data.action = value,
                        (data, info) -> data.action)
                .add()
                .append(new KeyedCodec<>("Item", Codec.STRING),
                        (data, value, info) -> data.item = value,
                        (data, info) -> data.item)
                .add()
                .append(new KeyedCodec<>("@Quantity", Codec.STRING),
                        (data, value, info) -> data.quantity = value,
                        (data, info) -> data.quantity)
                .add()
                .build();

        private String action;
        private String item;
        private String quantity;

        private PageData() {
        }
    }
}
