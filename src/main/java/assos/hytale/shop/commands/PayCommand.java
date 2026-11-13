package assos.hytale.shop.commands;

import assos.hytale.shop.ShopMessages;
import assos.hytale.shop.ShopPermissions;
import assos.hytale.shop.economy.EconomyService.TransferStatus;
import assos.hytale.shop.shop.ShopActions;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

/**
 * {@code /pay <player> <amount>} sends Hcoins to an online player.
 */
public final class PayCommand extends AbstractPlayerCommand {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ShopActions actions;
    private final RequiredArg<PlayerRef> playerArg;
    private final RequiredArg<Integer> amountArg;

    public PayCommand(@Nonnull ShopActions actions, boolean requirePermissions) {
        super("pay", "Send Hcoins to another player");
        ShopPermissions.apply(this, requirePermissions, ShopPermissions.PAY);
        this.actions = actions;
        this.playerArg = withRequiredArg("player", "Online player", ArgTypes.PLAYER_REF);
        this.amountArg = withRequiredArg("amount", "Hcoins to send", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        PlayerRef target = playerArg.get(context);
        int amount = amountArg.get(context);
        if (amount <= 0) {
            context.sendMessage(ShopMessages.error("The amount must be greater than zero."));
            return;
        }

        String money = actions.getMessages().money(amount);
        try {
            actions.touch(playerRef);
            actions.touch(target);
            TransferStatus status = actions.getEconomy().transfer(playerRef.getUuid(), target.getUuid(), amount);
            switch (status) {
                case SUCCESS -> {
                    context.sendMessage(ShopMessages.success("Sent " + money + " to " + target.getUsername()
                            + ". Balance: " + actions.getMessages().money(
                                    actions.getEconomy().getBalance(playerRef.getUuid())) + "."));
                    target.sendMessage(ShopMessages.success(playerRef.getUsername() + " sent you " + money + "."));
                }
                case INSUFFICIENT_FUNDS -> context.sendMessage(ShopMessages.error("You do not have " + money + "."));
                case SAME_ACCOUNT -> context.sendMessage(ShopMessages.error("You cannot pay yourself."));
            }
        } catch (IllegalArgumentException exception) {
            context.sendMessage(ShopMessages.error(exception.getMessage()));
        } catch (IOException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception).log("Failed to save a payment from %s to %s",
                    playerRef.getUsername(), target.getUsername());
            context.sendMessage(ShopMessages.error("The payment could not be saved. Check the server logs."));
        }
    }
}
