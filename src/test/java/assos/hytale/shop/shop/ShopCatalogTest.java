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

    private static final List<ShopEntry> AUTO = List.of(
            new ShopEntry("Soil_Dirt", 2, 1),
            new ShopEntry("Rock_Stone", 3, 1),
            new ShopEntry("Wood_Oak_Trunk", 4, 1));

    @TempDir
    Path tempDir;

    private ShopCatalog loaded(Path file) throws IOException {
        ShopCatalog catalog = new ShopCatalog(file);
        catalog.setAutoEntries(AUTO);
        catalog.load();
        return catalog;
    }

    private static List<String> ids(ShopCatalog catalog) {
        return catalog.list().stream().map(ShopEntry::itemId).toList();
    }

    @Test
    void missingFileStartsWithOnlyTheAutomaticEntriesAndCreatesTheFile() throws Exception {
        Path file = tempDir.resolve("shop.json");

        ShopCatalog catalog = loaded(file);

        assertEquals(List.of("Rock_Stone", "Soil_Dirt", "Wood_Oak_Trunk"), ids(catalog));
        assertTrue(Files.exists(file));
    }

    @Test
    void withoutAutomaticEntriesTheShopStartsEmpty() throws Exception {
        ShopCatalog catalog = new ShopCatalog(tempDir.resolve("shop.json"));

        catalog.load();

        assertTrue(catalog.list().isEmpty());
    }

    @Test
    void listIsSortedByIdIgnoringCase() throws Exception {
        ShopCatalog catalog = loaded(tempDir.resolve("shop.json"));

        catalog.put(new ShopEntry("apple_Custom", 5, 1));
        catalog.put(new ShopEntry("Zinc_Custom", 5, 1));

        assertEquals(List.of("apple_Custom", "Rock_Stone", "Soil_Dirt", "Wood_Oak_Trunk", "Zinc_Custom"),
                ids(catalog));
    }

    @Test
    void putOverridesAnAutomaticPriceAndSurvivesAReload() throws Exception {
        Path file = tempDir.resolve("shop.json");
        ShopCatalog catalog = loaded(file);

        assertFalse(catalog.put(new ShopEntry("Rock_Stone", 9, 0)));

        assertEquals(new ShopEntry("Rock_Stone", 9, 0), catalog.find("Rock_Stone").orElseThrow());
        assertTrue(catalog.isOverridden("Rock_Stone"));
        assertEquals(new ShopEntry("Rock_Stone", 9, 0), loaded(file).find("Rock_Stone").orElseThrow());
    }

    @Test
    void putAddsANonBlockItem() throws Exception {
        ShopCatalog catalog = loaded(tempDir.resolve("shop.json"));

        assertTrue(catalog.put(new ShopEntry("Ingredient_Bar_Iron", 30, 10)));

        assertEquals(4, catalog.list().size());
    }

    @Test
    void findIsExactFirstThenCaseInsensitive() throws Exception {
        ShopCatalog catalog = loaded(tempDir.resolve("shop.json"));

        assertEquals("Rock_Stone", catalog.find("Rock_Stone").orElseThrow().itemId());
        assertEquals("Rock_Stone", catalog.find("ROCK_STONE").orElseThrow().itemId());
        assertTrue(catalog.find("Rock").isEmpty());
    }

    @Test
    void removingAnAutomaticItemExcludesItAcrossReloads() throws Exception {
        Path file = tempDir.resolve("shop.json");
        ShopCatalog catalog = loaded(file);

        assertTrue(catalog.remove("soil_dirt"));
        assertFalse(catalog.remove("soil_dirt"));

        assertTrue(catalog.find("Soil_Dirt").isEmpty());
        assertTrue(loaded(file).find("Soil_Dirt").isEmpty());
    }

    @Test
    void removingACustomItemDropsIt() throws Exception {
        ShopCatalog catalog = loaded(tempDir.resolve("shop.json"));
        catalog.put(new ShopEntry("Ingredient_Bar_Iron", 30, 10));

        assertTrue(catalog.remove("Ingredient_Bar_Iron"));

        assertTrue(catalog.find("Ingredient_Bar_Iron").isEmpty());
        assertFalse(catalog.reset("Ingredient_Bar_Iron"));
    }

    @Test
    void putBringsBackAnExcludedItem() throws Exception {
        ShopCatalog catalog = loaded(tempDir.resolve("shop.json"));
        catalog.remove("Soil_Dirt");

        assertTrue(catalog.put(new ShopEntry("Soil_Dirt", 7, 2)));

        assertEquals(new ShopEntry("Soil_Dirt", 7, 2), catalog.find("Soil_Dirt").orElseThrow());
    }

    @Test
    void resetRestoresTheAutomaticPriceAndUndoesAnExclusion() throws Exception {
        Path file = tempDir.resolve("shop.json");
        ShopCatalog catalog = loaded(file);
        catalog.put(new ShopEntry("Rock_Stone", 9, 0));
        catalog.remove("Soil_Dirt");

        assertTrue(catalog.reset("rock_stone"));
        assertTrue(catalog.reset("Soil_Dirt"));
        assertFalse(catalog.reset("Wood_Oak_Trunk"));

        assertEquals(new ShopEntry("Rock_Stone", 3, 1), catalog.find("Rock_Stone").orElseThrow());
        assertEquals(new ShopEntry("Soil_Dirt", 2, 1), loaded(file).find("Soil_Dirt").orElseThrow());
    }

    @Test
    void categoriesComeFromTheCategorizer() throws Exception {
        ShopCatalog catalog = loaded(tempDir.resolve("shop.json"));

        catalog.setCategorizer(id -> id.startsWith("Rock") ? ShopCategory.ROCKS : null);

        assertEquals(ShopCategory.ROCKS, catalog.categoryOf("Rock_Stone"));
        assertEquals(ShopCategory.OTHER, catalog.categoryOf("Soil_Dirt"));
        assertEquals(ShopCategory.OTHER, catalog.categoryOf("Unknown"));
    }

    @Test
    void versionOneFilesAreReadAsCustomPrices() throws Exception {
        Path file = tempDir.resolve("shop.json");
        Files.writeString(file, """
                { "version": 1, "items": [ { "itemId": "Rock_Stone", "buyPrice": 5, "sellPrice": 2 } ] }
                """);

        ShopCatalog catalog = loaded(file);

        assertEquals(new ShopEntry("Rock_Stone", 5, 2), catalog.find("Rock_Stone").orElseThrow());
        assertEquals(3, catalog.list().size());
    }

    @Test
    void invalidEntriesAreSkipped() throws Exception {
        Path file = tempDir.resolve("shop.json");
        Files.writeString(file, """
                {
                  "version": 2,
                  "items": [
                    { "itemId": "Good", "buyPrice": 4, "sellPrice": 2 },
                    { "itemId": "", "buyPrice": 4, "sellPrice": 2 },
                    { "itemId": "Negative", "buyPrice": -1, "sellPrice": 2 },
                    { "itemId": "Free", "buyPrice": 0, "sellPrice": 0 },
                    null
                  ],
                  "excluded": [ "", null, "Wood_Oak_Trunk" ]
                }
                """);
        ShopCatalog catalog = new ShopCatalog(file);
        catalog.setAutoEntries(AUTO);

        catalog.load();

        assertEquals(List.of("Good", "Rock_Stone", "Soil_Dirt"), ids(catalog));
    }

    @Test
    void newerVersionIsRejected() throws Exception {
        Path file = tempDir.resolve("shop.json");
        Files.writeString(file, "{ \"version\": 3, \"items\": [] }");

        assertThrows(IOException.class, () -> new ShopCatalog(file).load());
    }

    @Test
    void failedLoadKeepsTheItemsAndRefusesEditsUntilAReloadSucceeds() throws Exception {
        Path file = tempDir.resolve("shop.json");
        ShopCatalog catalog = loaded(file);
        catalog.put(new ShopEntry("Rock_Stone", 9, 0));
        Files.writeString(file, "{ broken");

        assertThrows(IOException.class, catalog::load);
        assertEquals(new ShopEntry("Rock_Stone", 9, 0), catalog.find("Rock_Stone").orElseThrow());
        assertThrows(IOException.class, () -> catalog.put(new ShopEntry("X", 1, 1)));
        assertThrows(IOException.class, () -> catalog.remove("Soil_Dirt"));
        assertThrows(IOException.class, () -> catalog.reset("Rock_Stone"));
        assertEquals("{ broken", Files.readString(file));
        assertTrue(catalog.find("Soil_Dirt").isPresent());

        Files.writeString(file, "{ \"version\": 2, \"items\": [] }");
        catalog.load();
        assertTrue(catalog.put(new ShopEntry("X", 1, 1)));
    }
}
