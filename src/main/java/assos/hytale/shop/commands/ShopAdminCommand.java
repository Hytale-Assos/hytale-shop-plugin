package assos.hytale.shop.commands;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.ShopPermissions;
import assos.hytale.shop.shop.ShopActions;
import assos.hytale.shop.shop.ShopCatalog;
import assos.hytale.shop.shop.ShopEntry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Admin command: {@code /shopadmin set|remove|reload}. Always requires {@link ShopPermissions#ADMIN}.
 */
public final class ShopAdminCommand extends AbstractCommandCollection {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ShopAdminCommand(@Nonnull ShopActions actions) {
        super("shopadmin", "Manage the items of the shop (admin)");
        requirePermission(ShopPermissions.ADMIN);
        ShopCatalog catalog = actions.getShopService().getCatalog();
        addSubCommand(new SetCommand(catalog, actions.getMessages()));
        addSubCommand(new RemoveCommand(catalog));
        addSubCommand(new ReloadCommand(catalog));
    }

    private static final class SetCommand extends AbstractCommand {

        private final ShopCatalog catalog;
        private final ShopMessages messages;
        private final RequiredArg<Item> itemArg;
        private final RequiredArg<Integer> buyArg;
        private final RequiredArg<Integer> sellArg;

        private SetCommand(ShopCatalog catalog, ShopMessages messages) {
            super("set", "Add an item to the shop or change its prices (0 disables a direction)");
            requirePermission(ShopPermissions.ADMIN);
            this.catalog = catalog;
            this.messages = messages;
            this.itemArg = withRequiredArg("item", "Item id", ArgTypes.ITEM_ASSET);
            this.buyArg = withRequiredArg("buyPrice", "Price paid by players per unit, 0 to disable",
                    ArgTypes.INTEGER);
            this.sellArg = withRequiredArg("sellPrice", "Price paid to players per unit, 0 to disable",
                    ArgTypes.INTEGER);
        }

        @Nullable
        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            Item item = itemArg.get(context);
            int buyPrice = buyArg.get(context);
            int sellPrice = sellArg.get(context);
            try {
                ShopEntry entry = new ShopEntry(item.getId(), buyPrice, sellPrice);
                if (sellPrice > buyPrice && buyPrice > 0) {
                    context.sendMessage(ShopMessages.error("Warning: the sell price is higher than the buy price, "
                            + "players can make infinite Hcoins by buying and selling back."));
                }
                boolean created = catalog.put(entry);
                context.sendMessage(ShopMessages.success((created ? "Added " : "Updated ") + entry.itemId()
                        + " - buy: " + (entry.canBuy() ? messages.money(buyPrice) : "disabled")
                        + " - sell: " + (entry.canSell() ? messages.money(sellPrice) : "disabled") + "."));
            } catch (IllegalArgumentException exception) {
                context.sendMessage(ShopMessages.error(exception.getMessage()));
            } catch (IOException exception) {
                LOGGER.at(Level.SEVERE).withCause(exception).log("Failed to save the shop");
                context.sendMessage(ShopMessages.error("The shop could not be saved. Check the server logs."));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class RemoveCommand extends AbstractCommand {

        private final ShopCatalog catalog;
        private final RequiredArg<String> itemArg;

        private RemoveCommand(ShopCatalog catalog) {
            super("remove", "Remove an item from the shop");
            requirePermission(ShopPermissions.ADMIN);
            this.catalog = catalog;
            this.itemArg = withRequiredArg("item", "Item id", ArgTypes.STRING);
            this.itemArg.suggest((sender, input, offset, result) -> ShopSuggestions.suggestItems(
                    catalog, entry -> true, input, result));
        }

        @Nullable
        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            String itemId = itemArg.get(context);
            try {
                if (catalog.remove(itemId)) {
                    context.sendMessage(ShopMessages.success("Removed " + itemId + " from the shop."));
                } else {
                    context.sendMessage(ShopMessages.error("'" + itemId + "' is not in the shop."));
                }
            } catch (IOException exception) {
                LOGGER.at(Level.SEVERE).withCause(exception).log("Failed to save the shop");
                context.sendMessage(ShopMessages.error("The shop could not be saved. Check the server logs."));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ReloadCommand extends AbstractCommand {

        private final ShopCatalog catalog;

        private ReloadCommand(ShopCatalog catalog) {
            super("reload", "Reload shop.json from disk");
            requirePermission(ShopPermissions.ADMIN);
            this.catalog = catalog;
        }

        @Nullable
        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            try {
                catalog.load();
                context.sendMessage(ShopMessages.success("Shop reloaded: " + catalog.list().size() + " items."));
            } catch (IOException exception) {
                LOGGER.at(Level.SEVERE).withCause(exception).log("Failed to reload the shop");
                context.sendMessage(ShopMessages.error(
                        "Could not reload shop.json, the previous items are kept. Check the server logs."));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
