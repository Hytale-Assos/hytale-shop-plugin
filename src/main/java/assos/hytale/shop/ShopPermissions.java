package assos.hytale.shop;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;

public final class ShopPermissions {

    public static final String SHOP = "hytale.shop.shop";
    public static final String BALANCE = "hytale.shop.balance";
    public static final String BALANCE_OTHERS = "hytale.shop.balance.others";
    public static final String PAY = "hytale.shop.pay";
    public static final String TOP = "hytale.shop.top";
    public static final String ADMIN = "hytale.shop.admin";

    private ShopPermissions() {
    }

    /**
     * Player commands are open to everyone unless {@code RequirePermissions} is enabled in the config.
     */
    public static void apply(AbstractCommand command, boolean requirePermissions, String permission) {
        if (requirePermissions) {
            command.requirePermission(permission);
        } else {
            command.requireNoPermission();
        }
    }
}
