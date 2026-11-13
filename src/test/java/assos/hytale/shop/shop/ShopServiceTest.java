package assos.hytale.shop.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import assos.hytale.shop.economy.AccountStorage;
import assos.hytale.shop.economy.EconomyService;
import assos.hytale.shop.shop.ShopService.TradeResult;
import assos.hytale.shop.shop.ShopService.TradeStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShopServiceTest {

    private static final String STONE = "Rock_Stone";
    private static final String DIRT = "Soil_Dirt";
    private static final String BUY_ONLY = "Rock_Marble";
    private static final String SELL_ONLY = "Ore_Iron_Stone";

    @TempDir
    Path tempDir;

    private final UUID player = UUID.randomUUID();
    private EconomyService economy;
    private ShopService shop;

    @BeforeEach
    void setUp() throws Exception {
        economy = new EconomyService(new AccountStorage(tempDir.resolve("accounts.json")), 100);
        ShopCatalog catalog = new ShopCatalog(tempDir.resolve("shop.json"));
        catalog.load();
        catalog.put(new ShopEntry(STONE, 3, 1));
        catalog.put(new ShopEntry(DIRT, 2, 1));
        catalog.put(new ShopEntry(BUY_ONLY, 10, 0));
        catalog.put(new ShopEntry(SELL_ONLY, 0, 7));
        shop = new ShopService(catalog, economy, 64);
    }

    @Test
    void maxQuantityMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShopService(shop.getCatalog(), economy, 0));
    }

    @Test
    void buyTakesHcoinsAndGivesItems() throws Exception {
        FakeInventory inventory = new FakeInventory(100);

        TradeResult result = shop.buy(player, inventory, STONE, 10);

        assertTrue(result.succeeded());
        assertEquals(30, result.total());
        assertEquals(70, result.balance());
        assertEquals(70, economy.getBalance(player));
        assertEquals(10, inventory.count(STONE));
    }

    @Test
    void buyIsCaseInsensitiveOnTheItemId() throws Exception {
        FakeInventory inventory = new FakeInventory(100);

        TradeResult result = shop.buy(player, inventory, "rock_stone", 1);

        assertTrue(result.succeeded());
        assertEquals(STONE, result.itemId());
        assertEquals(1, inventory.count(STONE));
    }

    @Test
    void buyWithExactlyEnoughHcoinsSucceeds() throws Exception {
        FakeInventory inventory = new FakeInventory(100);

        TradeResult result = shop.buy(player, inventory, BUY_ONLY, 10);

        assertTrue(result.succeeded());
        assertEquals(0, economy.getBalance(player));
    }

    @Test
    void buyWithoutEnoughHcoinsChangesNothing() throws Exception {
        FakeInventory inventory = new FakeInventory(100);

        TradeResult result = shop.buy(player, inventory, BUY_ONLY, 11);

        assertEquals(TradeStatus.INSUFFICIENT_FUNDS, result.status());
        assertEquals(110, result.total());
        assertEquals(100, economy.getBalance(player));
        assertEquals(0, inventory.count(BUY_ONLY));
    }

    @Test
    void buyWithAFullInventoryChangesNothing() throws Exception {
        FakeInventory inventory = new FakeInventory(5);

        TradeResult result = shop.buy(player, inventory, STONE, 6);

        assertEquals(TradeStatus.INVENTORY_FULL, result.status());
        assertEquals(100, economy.getBalance(player));
        assertEquals(0, inventory.count(STONE));
    }

    @Test
    void buyRefundsWhenGivingTheItemsFails() throws Exception {
        FakeInventory inventory = new FakeInventory(100);
        inventory.failNextAdd = true;

        TradeResult result = shop.buy(player, inventory, STONE, 5);

        assertEquals(TradeStatus.INVENTORY_FULL, result.status());
        assertEquals(100, economy.getBalance(player));
        assertEquals(0, inventory.count(STONE));
    }

    @Test
    void buyOfAnItemOutsideTheShopFails() throws Exception {
        TradeResult result = shop.buy(player, new FakeInventory(100), "Unknown_Item", 1);

        assertEquals(TradeStatus.NOT_IN_SHOP, result.status());
        assertEquals(100, economy.getBalance(player));
    }

    @Test
    void buyOfASellOnlyItemFails() throws Exception {
        TradeResult result = shop.buy(player, new FakeInventory(100), SELL_ONLY, 1);

        assertEquals(TradeStatus.NOT_FOR_SALE, result.status());
    }

    @Test
    void buyRejectsInvalidQuantities() throws Exception {
        FakeInventory inventory = new FakeInventory(1000);

        assertEquals(TradeStatus.INVALID_QUANTITY, shop.buy(player, inventory, STONE, 0).status());
        assertEquals(TradeStatus.INVALID_QUANTITY, shop.buy(player, inventory, STONE, -3).status());
        assertEquals(TradeStatus.QUANTITY_TOO_LARGE, shop.buy(player, inventory, STONE, 65).status());
        assertTrue(shop.buy(player, inventory, DIRT, 1).succeeded());
        assertEquals(98, economy.getBalance(player));
    }
}
