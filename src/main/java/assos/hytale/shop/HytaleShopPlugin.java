package assos.hytale.shop;

import assos.hytale.shop.config.ShopConfig;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;

public final class HytaleShopPlugin extends JavaPlugin {

    private static final String CONFIG_NAME = "shop_config";

    private final Config<ShopConfig> config;

    public HytaleShopPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = this.withConfig(CONFIG_NAME, ShopConfig.CODEC);
    }

    @Override
    protected void setup() {
        config.save();
    }
}
