package assos.hytale.shop.commands;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.ShopPermissions;
import assos.hytale.shop.economy.EconomyService.TopEntry;
import assos.hytale.shop.shop.ShopActions;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /hcointop} (alias {@code /baltop}) lists the richest accounts.
 */
public final class HcoinTopCommand extends AbstractCommand {

    private final ShopActions actions;
    private final int size;

    public HcoinTopCommand(@Nonnull ShopActions actions, int size, boolean requirePermissions) {
        super("hcointop", "Show the richest players");
        addAliases("baltop");
        ShopPermissions.apply(this, requirePermissions, ShopPermissions.TOP);
        this.actions = actions;
        this.size = size;
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        List<TopEntry> top = actions.getEconomy().top(size);
        if (top.isEmpty()) {
            context.sendMessage(ShopMessages.info("No account yet."));
            return CompletableFuture.completedFuture(null);
        }
        context.sendMessage(ShopMessages.highlight("Richest players:"));
        for (int index = 0; index < top.size(); index++) {
            TopEntry entry = top.get(index);
            String name = entry.name() != null ? entry.name() : entry.playerId().toString();
            context.sendMessage(ShopMessages.info(" " + (index + 1) + ". " + name + " - "
                    + actions.getMessages().money(entry.balance())));
        }
        return CompletableFuture.completedFuture(null);
    }
}
