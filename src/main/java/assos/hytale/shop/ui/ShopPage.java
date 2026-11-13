package assos.hytale.shop.ui;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.shop.HytalePlayerInventory;
import assos.hytale.shop.shop.ShopActions;
import assos.hytale.shop.shop.ShopCatalog;
import assos.hytale.shop.shop.ShopCategory;
import assos.hytale.shop.shop.ShopEntry;
import assos.hytale.shop.shop.ShopFilter;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
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
import java.util.Map;
import java.util.logging.Level;

/**
 * The shop window opened by {@code /shop}. Only one page of {@link #PAGE_SIZE} items is sent to the client at a
 * time, filtered by category tab and search text, so the window stays fast with thousands of blocks.
 */
public final class ShopPage extends InteractiveCustomUIPage<ShopPage.PageData> {

    public static final int PAGE_SIZE = 25;

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PAGE_LAYOUT = "Pages/HytaleShop/ShopPage.ui";
    private static final String ROW_LAYOUT = "Pages/HytaleShop/ShopItemRow.ui";
    private static final String ALL_CATEGORIES = "All";

    private static final String ACTION_BUY = "Buy";
    private static final String ACTION_SELL = "Sell";
    private static final String ACTION_SELL_ALL = "SellAll";
    private static final String ACTION_CATEGORY = "Category";
    private static final String ACTION_SEARCH = "Search";
    private static final String ACTION_PREV = "Prev";
    private static final String ACTION_NEXT = "Next";

    private final ShopActions actions;
    private Message status;
    private String quantity = "1";
    private String search = "";
    private ShopCategory category;
    private int page;

    public ShopPage(@Nonnull PlayerRef playerRef, @Nonnull ShopActions actions) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.actions = actions;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        ShopMessages messages = actions.getMessages();
        ShopCatalog catalog = actions.getShopService().getCatalog();

        builder.append(PAGE_LAYOUT);
        builder.set("#ShopTitle.Text", messages.getCurrencyName() + " Shop");
        builder.set("#Balance.Text",
                "Balance: " + messages.money(actions.getEconomy().getBalance(playerRef.getUuid())));
        builder.set("#QuantityInput.Value", quantity);
        builder.set("#SearchInput.Value", search);
        builder.set("#Status.TextSpans", status != null ? status : Message.raw(""));

        bindTabs(builder, eventBuilder);
        bindAction(eventBuilder, "#SearchButton", ACTION_SEARCH, null);
        bindAction(eventBuilder, "#PrevButton", ACTION_PREV, null);
        bindAction(eventBuilder, "#NextButton", ACTION_NEXT, null);

        List<ShopEntry> matches = ShopFilter.filter(catalog.list(), catalog::categoryOf, category, search).stream()
                .filter(entry -> ShopMessages.isKnownItem(entry.itemId()))
                .toList();
        int pageCount = ShopFilter.pageCount(matches.size(), PAGE_SIZE);
        page = Math.min(Math.max(page, 0), pageCount - 1);
        builder.set("#PageLabel.Text", matches.isEmpty()
                ? "No item"
                : "Page " + (page + 1) + " / " + pageCount + "  (" + matches.size() + " items)");
        builder.set("#PrevButton.Disabled", page == 0);
        builder.set("#NextButton.Disabled", page >= pageCount - 1);

        if (matches.isEmpty()) {
            if (status == null) {
                builder.set("#Status.TextSpans", ShopMessages.info("No item matches this tab and search."));
            }
            return;
        }

        HytalePlayerInventory inventory = HytalePlayerInventory.of(store, ref);
        Map<String, Integer> owned = inventory == null ? Map.of() : inventory.countAll();
        List<ShopEntry> visible = ShopFilter.page(matches, page, PAGE_SIZE);
        for (int index = 0; index < visible.size(); index++) {
            ShopEntry entry = visible.get(index);
            String row = "#ItemList[" + index + "]";
            int count = owned.getOrDefault(entry.itemId(), 0);
            builder.append("#ItemList", ROW_LAYOUT);
            builder.set(row + " #Icon.ItemId", entry.itemId());
            builder.set(row + " #Name.TextSpans", ShopMessages.itemName(entry.itemId()));
            builder.set(row + " #Prices.Text", "Buy: " + (entry.canBuy() ? messages.money(entry.buyPrice()) : "-")
                    + "   Sell: " + (entry.canSell() ? messages.money(entry.sellPrice()) : "-"));
            builder.set(row + " #Owned.Text", "In inventory: " + count + "   (" + entry.itemId() + ")");
            builder.set(row + " #BuyButton.Disabled", !entry.canBuy());
            builder.set(row + " #SellButton.Disabled", !entry.canSell() || count == 0);
            builder.set(row + " #SellAllButton.Disabled", !entry.canSell() || count == 0);

            bindAction(eventBuilder, row + " #BuyButton", ACTION_BUY, entry.itemId());
            bindAction(eventBuilder, row + " #SellButton", ACTION_SELL, entry.itemId());
            bindAction(eventBuilder, row + " #SellAllButton", ACTION_SELL_ALL, entry.itemId());
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
            @Nonnull PageData data) {
        try {
            if (data.quantity != null) {
                quantity = data.quantity.trim();
            }
            if (data.search != null) {
                search = data.search.trim();
            }
            if (data.action == null) {
                return;
            }
            handleAction(ref, store, data);
        } catch (RuntimeException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Shop window action failed for %s", playerRef.getUsername());
            status = ShopMessages.error("Something went wrong. Check the server logs.");
        }
        refresh(ref, store);
    }

    private void handleAction(Ref<EntityStore> ref, Store<EntityStore> store, PageData data) {
        switch (data.action) {
            case ACTION_BUY, ACTION_SELL -> {
                if (data.value == null) {
                    return;
                }
                int amount = parseQuantity();
                if (amount <= 0) {
                    status = invalidQuantity();
                } else if (ACTION_BUY.equals(data.action)) {
                    status = actions.buy(playerRef, store, ref, data.value, amount);
                } else {
                    status = actions.sell(playerRef, store, ref, data.value, amount);
                }
            }
            case ACTION_SELL_ALL -> {
                if (data.value != null) {
                    status = actions.sellAll(playerRef, store, ref, data.value);
                }
            }
            case ACTION_CATEGORY -> {
                category = ALL_CATEGORIES.equals(data.value) ? null : ShopCategory.fromLabel(data.value);
                page = 0;
                status = null;
            }
            case ACTION_SEARCH -> {
                page = 0;
                status = null;
            }
            case ACTION_PREV -> page--;
            case ACTION_NEXT -> page++;
            default -> {
            }
        }
    }

    private void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder builder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        build(ref, builder, eventBuilder, store);
        sendUpdate(builder, eventBuilder, true);
    }

    private void bindTabs(UICommandBuilder builder, UIEventBuilder eventBuilder) {
        builder.set("#CatAll.Disabled", category == null);
        bindAction(eventBuilder, "#CatAll", ACTION_CATEGORY, ALL_CATEGORIES);
        for (ShopCategory tab : ShopCategory.values()) {
            String selector = "#Cat" + tab.getLabel();
            builder.set(selector + ".Disabled", tab == category);
            bindAction(eventBuilder, selector, ACTION_CATEGORY, tab.getLabel());
        }
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

    private static void bindAction(UIEventBuilder eventBuilder, String selector, String action, String value) {
        EventData data = EventData.of("Action", action)
                .append("@Quantity", "#QuantityInput.Value")
                .append("@Search", "#SearchInput.Value");
        if (value != null) {
            data = data.append("Value", value);
        }
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, data, false);
    }

    public static final class PageData {

        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (data, value, info) -> data.action = value,
                        (data, info) -> data.action)
                .add()
                .append(new KeyedCodec<>("Value", Codec.STRING),
                        (data, value, info) -> data.value = value,
                        (data, info) -> data.value)
                .add()
                .append(new KeyedCodec<>("@Quantity", Codec.STRING),
                        (data, value, info) -> data.quantity = value,
                        (data, info) -> data.quantity)
                .add()
                .append(new KeyedCodec<>("@Search", Codec.STRING),
                        (data, value, info) -> data.search = value,
                        (data, info) -> data.search)
                .add()
                .build();

        private String action;
        private String value;
        private String quantity;
        private String search;

        private PageData() {
        }
    }
}
