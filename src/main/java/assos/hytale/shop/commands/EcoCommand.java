package assos.hytale.shop.commands;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.ShopPermissions;
import assos.hytale.shop.economy.EconomyService;
import assos.hytale.shop.shop.ShopActions;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Admin command: {@code /eco give|take|set <player> <amount>}. Always requires {@link ShopPermissions#ADMIN}.
 */
public final class EcoCommand extends AbstractCommandCollection {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public EcoCommand(@Nonnull ShopActions actions) {
        super("eco", "Manage Hcoin balances (admin)");
        requirePermission(ShopPermissions.ADMIN);
        addSubCommand(new AmountCommand("give", "Give Hcoins to a player", actions, Operation.GIVE));
        addSubCommand(new AmountCommand("take", "Take Hcoins from a player", actions, Operation.TAKE));
        addSubCommand(new AmountCommand("set", "Set the Hcoin balance of a player", actions, Operation.SET));
    }

    private enum Operation {
        GIVE,
        TAKE,
        SET
    }

    private static final class AmountCommand extends AbstractCommand {

        private final ShopActions actions;
        private final Operation operation;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<Integer> amountArg;

        private AmountCommand(String name, String description, ShopActions actions, Operation operation) {
            super(name, description);
            requirePermission(ShopPermissions.ADMIN);
            this.actions = actions;
            this.operation = operation;
            this.playerArg = withRequiredArg("player", "Online player", ArgTypes.PLAYER_REF);
            this.amountArg = withRequiredArg("amount", "Hcoins", ArgTypes.INTEGER);
        }

        @Nullable
        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            PlayerRef target = playerArg.get(context);
            int amount = amountArg.get(context);
            EconomyService economy = actions.getEconomy();
            ShopMessages messages = actions.getMessages();
            try {
                actions.touch(target);
                switch (operation) {
                    case GIVE -> {
                        long balance = economy.deposit(target.getUuid(), amount);
                        context.sendMessage(ShopMessages.success("Gave " + messages.money(amount) + " to "
                                + target.getUsername() + ". New balance: " + messages.money(balance) + "."));
                        target.sendMessage(ShopMessages.success("You received " + messages.money(amount) + "."));
                    }
                    case TAKE -> {
                        long removed = economy.take(target.getUuid(), amount);
                        context.sendMessage(ShopMessages.success("Took " + messages.money(removed) + " from "
                                + target.getUsername() + ". New balance: "
                                + messages.money(economy.getBalance(target.getUuid())) + "."));
                    }
                    case SET -> {
                        economy.setBalance(target.getUuid(), amount);
                        context.sendMessage(ShopMessages.success("Set the balance of " + target.getUsername()
                                + " to " + messages.money(amount) + "."));
                    }
                }
            } catch (IllegalArgumentException exception) {
                context.sendMessage(ShopMessages.error(exception.getMessage()));
            } catch (IOException exception) {
                LOGGER.at(Level.SEVERE).withCause(exception)
                        .log("Failed to save the balance of %s", target.getUsername());
                context.sendMessage(ShopMessages.error("The balance could not be saved. Check the server logs."));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
