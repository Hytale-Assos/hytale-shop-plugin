package assos.hytale.shop.commands;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.ShopPermissions;
import assos.hytale.shop.shop.ShopActions;
import assos.hytale.shop.shop.ShopCatalog;
import assos.hytale.shop.shop.ShopEntry;
import assos.hytale.shop.shop.ShopFilter;
import assos.hytale.shop.ui.ShopPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

/**
 * {@code /shop} opens the shop window; {@code /shop buy|sell|sellall|list|search} work from the chat.
 */
public final class ShopCommand extends AbstractPlayerCommand {

    private final ShopActions actions;

    public ShopCommand(@Nonnull ShopActions actions, boolean requirePermissions) {
        super("shop", "Open the Hcoin shop");
        ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
        this.actions = actions;
        addSubCommand(new BuyCommand(actions, requirePermissions));
        addSubCommand(new SellCommand(actions, requirePermissions));
        addSubCommand(new SellAllCommand(actions, requirePermissions));
        addSubCommand(new ListCommand(actions, requirePermissions));
        addSubCommand(new SearchCommand(actions, requirePermissions));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.sendMessage(ShopMessages.error("Could not open the shop."));
            return;
        }
        actions.touch(playerRef);
        player.getPageManager().openCustomPage(ref, store, new ShopPage(playerRef, actions));
    }

    private static RequiredArg<String> itemArg(AbstractPlayerCommand command, ShopActions actions,
            Predicate<ShopEntry> filter) {
        RequiredArg<String> arg = command.withRequiredArg("item", "Item id, see /shop list", ArgTypes.STRING);
        arg.suggest((sender, input, offset, result) -> ShopSuggestions.suggestItems(
                actions.getShopService().getCatalog(), filter, input, result));
        return arg;
    }

    private static final class BuyCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<String> itemArg;

        private BuyCommand(ShopActions actions, boolean requirePermissions) {
            super("buy", "Buy one item from the shop");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            this.itemArg = itemArg(this, actions, ShopEntry::canBuy);
            addUsageVariant(new BuyQuantityCommand(actions, requirePermissions));
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            context.sendMessage(actions.buy(playerRef, store, ref, itemArg.get(context), 1));
        }
    }

    private static final class BuyQuantityCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<String> itemArg;
        private final RequiredArg<Integer> quantityArg;

        private BuyQuantityCommand(ShopActions actions, boolean requirePermissions) {
            super("Buy several items from the shop");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            this.itemArg = itemArg(this, actions, ShopEntry::canBuy);
            this.quantityArg = withRequiredArg("quantity", "How many to buy", ArgTypes.INTEGER);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            context.sendMessage(actions.buy(playerRef, store, ref, itemArg.get(context), quantityArg.get(context)));
        }
    }

    private static final class SellCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<String> itemArg;

        private SellCommand(ShopActions actions, boolean requirePermissions) {
            super("sell", "Sell one item to the shop");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            this.itemArg = itemArg(this, actions, ShopEntry::canSell);
            addUsageVariant(new SellQuantityCommand(actions, requirePermissions));
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            context.sendMessage(actions.sell(playerRef, store, ref, itemArg.get(context), 1));
        }
    }

    private static final class SellQuantityCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<String> itemArg;
        private final RequiredArg<Integer> quantityArg;

        private SellQuantityCommand(ShopActions actions, boolean requirePermissions) {
            super("Sell several items to the shop");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            this.itemArg = itemArg(this, actions, ShopEntry::canSell);
            this.quantityArg = withRequiredArg("quantity", "How many to sell", ArgTypes.INTEGER);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            context.sendMessage(actions.sell(playerRef, store, ref, itemArg.get(context), quantityArg.get(context)));
        }
    }

    private static final class SellAllCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<String> itemArg;

        private SellAllCommand(ShopActions actions, boolean requirePermissions) {
            super("sellall", "Sell every unit of an item you carry");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            this.itemArg = itemArg(this, actions, ShopEntry::canSell);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            context.sendMessage(actions.sellAll(playerRef, store, ref, itemArg.get(context)));
        }
    }

    private static final int CHAT_PAGE_SIZE = 12;

    private static void sendEntries(CommandContext context, ShopActions actions, PlayerRef playerRef,
            List<ShopEntry> matches, int requestedPage, String header, String nextHint) {
        ShopMessages messages = actions.getMessages();
        if (matches.isEmpty()) {
            context.sendMessage(ShopMessages.error("No item found."));
            return;
        }
        int pageCount = ShopFilter.pageCount(matches.size(), CHAT_PAGE_SIZE);
        int page = Math.min(Math.max(requestedPage, 1), pageCount);
        context.sendMessage(ShopMessages.highlight(header + " - page " + page + "/" + pageCount + " ("
                + matches.size() + " items) - balance: "
                + messages.money(actions.getEconomy().getBalance(playerRef.getUuid()))));
        for (ShopEntry entry : ShopFilter.page(matches, page - 1, CHAT_PAGE_SIZE)) {
            String buy = entry.canBuy() ? messages.money(entry.buyPrice()) : "-";
            String sell = entry.canSell() ? messages.money(entry.sellPrice()) : "-";
            context.sendMessage(Message.join(ShopMessages.info(" " + entry.itemId() + " ("),
                    ShopMessages.itemName(entry.itemId()).color(ShopMessages.INFO),
                    ShopMessages.info(") - buy: " + buy + " - sell: " + sell)));
        }
        if (page < pageCount) {
            context.sendMessage(ShopMessages.info(nextHint.replace("{page}", String.valueOf(page + 1))));
        }
        context.sendMessage(ShopMessages.info("Use /shop buy <item> [quantity] or /shop sell <item> [quantity]."));
    }

    private static List<ShopEntry> knownEntries(ShopActions actions, String search) {
        ShopCatalog catalog = actions.getShopService().getCatalog();
        return ShopFilter.filter(catalog.list(), catalog::categoryOf, null, search).stream()
                .filter(entry -> ShopMessages.isKnownItem(entry.itemId()))
                .toList();
    }

    private static final class ListCommand extends AbstractPlayerCommand {

        private final ShopActions actions;

        private ListCommand(ShopActions actions, boolean requirePermissions) {
            super("list", "List the items of the shop and their prices");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            addUsageVariant(new ListPageCommand(actions, requirePermissions));
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            sendEntries(context, actions, playerRef, knownEntries(actions, null), 1, "Shop",
                    "Next page: /shop list {page}");
        }
    }

    private static final class ListPageCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<Integer> pageArg;

        private ListPageCommand(ShopActions actions, boolean requirePermissions) {
            super("List one page of the shop");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            this.pageArg = withRequiredArg("page", "Page number", ArgTypes.INTEGER);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            sendEntries(context, actions, playerRef, knownEntries(actions, null), pageArg.get(context), "Shop",
                    "Next page: /shop list {page}");
        }
    }

    private static final class SearchCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<String> textArg;

        private SearchCommand(ShopActions actions, boolean requirePermissions) {
            super("search", "Find shop items whose id contains a word");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.SHOP);
            this.actions = actions;
            this.textArg = withRequiredArg("text", "Words of the item id, ex: oak", ArgTypes.GREEDY_STRING);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            String text = textArg.get(context);
            sendEntries(context, actions, playerRef, knownEntries(actions, text), 1, "Search '" + text + "'",
                    "More results: refine your search or open /shop.");
        }
    }
}
