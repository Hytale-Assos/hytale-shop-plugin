package assos.hytale.shop.commands;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.ShopPermissions;
import assos.hytale.shop.shop.ShopActions;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * {@code /hcoin} (aliases {@code /balance}, {@code /bal}) shows your balance; {@code /hcoin <player>} shows the
 * balance of an online player.
 */
public final class HcoinCommand extends AbstractPlayerCommand {

    private final ShopActions actions;

    public HcoinCommand(@Nonnull ShopActions actions, boolean requirePermissions) {
        super("hcoin", "Show your Hcoin balance");
        addAliases("balance", "bal");
        ShopPermissions.apply(this, requirePermissions, ShopPermissions.BALANCE);
        this.actions = actions;
        addUsageVariant(new OtherBalanceCommand(actions, requirePermissions));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        actions.touch(playerRef);
        long balance = actions.getEconomy().getBalance(playerRef.getUuid());
        context.sendMessage(ShopMessages.highlight("Balance: " + actions.getMessages().money(balance)));
    }

    private static final class OtherBalanceCommand extends AbstractPlayerCommand {

        private final ShopActions actions;
        private final RequiredArg<PlayerRef> playerArg;

        private OtherBalanceCommand(ShopActions actions, boolean requirePermissions) {
            super("Show the Hcoin balance of an online player");
            ShopPermissions.apply(this, requirePermissions, ShopPermissions.BALANCE_OTHERS);
            this.actions = actions;
            this.playerArg = withRequiredArg("player", "Online player", ArgTypes.PLAYER_REF);
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
            PlayerRef target = playerArg.get(context);
            long balance = actions.getEconomy().getBalance(target.getUuid());
            context.sendMessage(ShopMessages.highlight(
                    target.getUsername() + "'s balance: " + actions.getMessages().money(balance)));
        }
    }
}
