package assos.hytale.shop.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class ShopConfig {

    public static final BuilderCodec<ShopConfig> CODEC = BuilderCodec.builder(ShopConfig.class, ShopConfig::new)
            .append(new KeyedCodec<>("CurrencyName", Codec.STRING),
                    (config, value, info) -> config.currencyName = value,
                    (config, info) -> config.currencyName)
            .add()
            .append(new KeyedCodec<>("StartingBalance", Codec.INTEGER),
                    (config, value, info) -> config.startingBalance = value,
                    (config, info) -> config.startingBalance)
            .add()
            .append(new KeyedCodec<>("MaxTransactionQuantity", Codec.INTEGER),
                    (config, value, info) -> config.maxTransactionQuantity = value,
                    (config, info) -> config.maxTransactionQuantity)
            .add()
            .append(new KeyedCodec<>("TopListSize", Codec.INTEGER),
                    (config, value, info) -> config.topListSize = value,
                    (config, info) -> config.topListSize)
            .add()
            .append(new KeyedCodec<>("IncludeAllBlocks", Codec.BOOLEAN),
                    (config, value, info) -> config.includeAllBlocks = value,
                    (config, info) -> config.includeAllBlocks)
            .add()
            .append(new KeyedCodec<>("SellPricePercent", Codec.INTEGER),
                    (config, value, info) -> config.sellPricePercent = value,
                    (config, info) -> config.sellPricePercent)
            .add()
            .append(new KeyedCodec<>("RequirePermissions", Codec.BOOLEAN),
                    (config, value, info) -> config.requirePermissions = value,
                    (config, info) -> config.requirePermissions)
            .add()
            .build();

    private String currencyName = "Hcoin";
    private int startingBalance = 100;
    private int maxTransactionQuantity = 1000;
    private int topListSize = 10;
    private boolean includeAllBlocks = true;
    private int sellPricePercent = 25;
    private boolean requirePermissions;

    private ShopConfig() {
    }

    public String getCurrencyName() {
        return currencyName == null || currencyName.isBlank() ? "Hcoin" : currencyName;
    }

    public int getStartingBalance() {
        return Math.max(0, startingBalance);
    }

    public int getMaxTransactionQuantity() {
        return Math.max(1, maxTransactionQuantity);
    }

    public int getTopListSize() {
        return Math.max(1, topListSize);
    }

    public boolean isIncludeAllBlocks() {
        return includeAllBlocks;
    }

    public int getSellPricePercent() {
        return Math.min(Math.max(sellPricePercent, 0), 100);
    }

    public boolean isRequirePermissions() {
        return requirePermissions;
    }
}
