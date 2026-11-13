package assos.hytale.shop.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShopCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileStartsWithTheDefaultItemsAndWritesThem() throws Exception {
        Path file = tempDir.resolve("shop.json");
        ShopCatalog catalog = new ShopCatalog(file);

        catalog.load();

        assertEquals(ShopCatalog.DEFAULT_ENTRIES, catalog.list());
        assertTrue(Files.exists(file));
        ShopCatalog reloaded = new ShopCatalog(file);
        reloaded.load();
        assertEquals(ShopCatalog.DEFAULT_ENTRIES, reloaded.list());
    }

    @Test
    void putAddsNewItemsAtTheEndAndUpdatesInPlace() throws Exception {
        Path file = tempDir.resolve("shop.json");
        Files.writeString(file, "{ \"version\": 1, \"items\": [] }");
        ShopCatalog catalog = new ShopCatalog(file);
        catalog.load();

        assertTrue(catalog.put(new ShopEntry("A", 5, 1)));
        assertTrue(catalog.put(new ShopEntry("B", 6, 2)));
        assertFalse(catalog.put(new ShopEntry("A", 9, 0)));

        assertEquals(List.of(new ShopEntry("A", 9, 0), new ShopEntry("B", 6, 2)), catalog.list());
        ShopCatalog reloaded = new ShopCatalog(file);
        reloaded.load();
        assertEquals(catalog.list(), reloaded.list());
    }

    @Test
    void findIsExactFirstThenCaseInsensitive() throws Exception {
        Path file = tempDir.resolve("shop.json");
        Files.writeString(file, "{ \"version\": 1, \"items\": [] }");
        ShopCatalog catalog = new ShopCatalog(file);
        catalog.load();
        catalog.put(new ShopEntry("Rock_Stone", 3, 1));

        assertEquals("Rock_Stone", catalog.find("Rock_Stone").orElseThrow().itemId());
        assertEquals("Rock_Stone", catalog.find("ROCK_STONE").orElseThrow().itemId());
        assertTrue(catalog.find("Rock").isEmpty());
    }

    @Test
    void removeDeletesTheItemCaseInsensitively() throws Exception {
        Path file = tempDir.resolve("shop.json");
        ShopCatalog catalog = new ShopCatalog(file);
        catalog.load();

        assertTrue(catalog.remove("soil_dirt"));
        assertFalse(catalog.remove("soil_dirt"));

        ShopCatalog reloaded = new ShopCatalog(file);
        reloaded.load();
        assertTrue(reloaded.find("Soil_Dirt").isEmpty());
    }

    @Test
    void invalidEntriesAreSkipped() throws Exception {
        Path file = tempDir.resolve("shop.json");
        Files.writeString(file, """
                {
                  "version": 1,
                  "items": [
                    { "itemId": "Good", "buyPrice": 4, "sellPrice": 2 },
                    { "itemId": "", "buyPrice": 4, "sellPrice": 2 },
                    { "itemId": "Negative", "buyPrice": -1, "sellPrice": 2 },
                    { "itemId": "Free", "buyPrice": 0, "sellPrice": 0 },
                    null
                  ]
                }
                """);
        ShopCatalog catalog = new ShopCatalog(file);

        catalog.load();

        assertEquals(List.of(new ShopEntry("Good", 4, 2)), catalog.list());
    }

    @Test
    void newerVersionIsRejected() throws Exception {
        Path file = tempDir.resolve("shop.json");
        Files.writeString(file, "{ \"version\": 2, \"items\": [] }");

        assertThrows(IOException.class, () -> new ShopCatalog(file).load());
    }

}
