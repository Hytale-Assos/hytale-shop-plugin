package assos.hytale.shop;

import assos.hytale.shop.commands.HcoinCommand;
import assos.hytale.shop.commands.HcoinTopCommand;
import assos.hytale.shop.commands.PayCommand;
import assos.hytale.shop.config.ShopConfig;
import assos.hytale.shop.economy.AccountStorage;
import assos.hytale.shop.economy.EconomyService;
import assos.hytale.shop.shop.ShopActions;
import assos.hytale.shop.shop.ShopCatalog;
import assos.hytale.shop.shop.ShopService;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

public final class HytaleShopPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_NAME = "shop_config";
    private static final String ACCOUNTS_FILE = "accounts.json";
    private static final String SHOP_FILE = "shop.json";

    private final Config<ShopConfig> config;
    private ShopConfig settings;
    private EconomyService economy;
    private ShopCatalog catalog;

    public HytaleShopPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = this.withConfig(CONFIG_NAME, ShopConfig.CODEC);
    }

    @Override
    protected void setup() {
        config.save();

        this.settings = config.get();
        this.economy = new EconomyService(new AccountStorage(getDataDirectory().resolve(ACCOUNTS_FILE)),
                settings.getStartingBalance());
        this.catalog = new ShopCatalog(getDataDirectory().resolve(SHOP_FILE));
        ShopService shopService = new ShopService(catalog, economy, settings.getMaxTransactionQuantity());
        ShopActions actions = new ShopActions(shopService, economy, new ShopMessages(settings.getCurrencyName()));

        boolean requirePermissions = settings.isRequirePermissions();
        getCommandRegistry().registerCommand(new HcoinCommand(actions, requirePermissions));
        getCommandRegistry().registerCommand(new PayCommand(actions, requirePermissions));
        getCommandRegistry().registerCommand(
                new HcoinTopCommand(actions, settings.getTopListSize(), requirePermissions));
    }

    @Override
    protected void start() {
        try {
            economy.load();
        } catch (IOException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to load Hcoin accounts; starting with no account");
        }
        try {
            catalog.load();
        } catch (IOException exception) {
            LOGGER.at(Level.SEVERE).withCause(exception)
                    .log("Failed to load the shop; it stays empty until /shopadmin reload succeeds");
        }
        LOGGER.at(Level.INFO).log("Shop ready with %d items", catalog.list().size());
    }
}
