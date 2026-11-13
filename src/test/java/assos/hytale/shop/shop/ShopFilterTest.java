package assos.hytale.shop.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ShopFilterTest {

    private static final List<ShopEntry> ENTRIES = List.of(
            new ShopEntry("Rock_Stone", 3, 1),
            new ShopEntry("Rock_Stone_Brick", 6, 2),
            new ShopEntry("Wood_Oak_Planks", 4, 1),
            new ShopEntry("Wood_Oak_Trunk", 4, 1),
            new ShopEntry("Soil_Dirt", 2, 1));

    private static final Function<String, ShopCategory> CATEGORY = id -> id.startsWith("Rock")
            ? ShopCategory.ROCKS
            : id.startsWith("Wood") ? ShopCategory.WOOD : ShopCategory.SOILS;

    private static List<String> ids(List<ShopEntry> entries) {
        return entries.stream().map(ShopEntry::itemId).toList();
    }

    @Test
    void noCategoryAndNoSearchKeepsEverything() {
        assertEquals(ENTRIES, ShopFilter.filter(ENTRIES, CATEGORY, null, null));
        assertEquals(ENTRIES, ShopFilter.filter(ENTRIES, CATEGORY, null, "   "));
    }

    @Test
    void categoryKeepsOnlyThatTab() {
        assertEquals(List.of("Wood_Oak_Planks", "Wood_Oak_Trunk"),
                ids(ShopFilter.filter(ENTRIES, CATEGORY, ShopCategory.WOOD, null)));
        assertTrue(ShopFilter.filter(ENTRIES, CATEGORY, ShopCategory.ORES, null).isEmpty());
    }

    @Test
    void searchIgnoresCaseAndRequiresEveryWord() {
        assertEquals(List.of("Rock_Stone", "Rock_Stone_Brick"),
                ids(ShopFilter.filter(ENTRIES, CATEGORY, null, "STONE")));
        assertEquals(List.of("Wood_Oak_Planks"), ids(ShopFilter.filter(ENTRIES, CATEGORY, null, "oak plank")));
        assertEquals(List.of("Rock_Stone_Brick"), ids(ShopFilter.filter(ENTRIES, CATEGORY, null, "stone_brick")));
        assertTrue(ShopFilter.filter(ENTRIES, CATEGORY, null, "diamond").isEmpty());
    }

    @Test
    void searchAndCategoryCombine() {
        assertTrue(ShopFilter.filter(ENTRIES, CATEGORY, ShopCategory.WOOD, "stone").isEmpty());
        assertEquals(List.of("Wood_Oak_Trunk"), ids(ShopFilter.filter(ENTRIES, CATEGORY, ShopCategory.WOOD, "trunk")));
    }

    @Test
    void pageCountIsAtLeastOne() {
        assertEquals(1, ShopFilter.pageCount(0, 25));
        assertEquals(1, ShopFilter.pageCount(25, 25));
        assertEquals(2, ShopFilter.pageCount(26, 25));
    }

    @Test
    void pageReturnsTheSliceAndClampsOutOfRangePages() {
        List<Integer> numbers = IntStream.range(0, 7).boxed().toList();

        assertEquals(List.of(0, 1, 2), ShopFilter.page(numbers, 0, 3));
        assertEquals(List.of(6), ShopFilter.page(numbers, 2, 3));
        assertEquals(List.of(6), ShopFilter.page(numbers, 99, 3));
        assertEquals(List.of(0, 1, 2), ShopFilter.page(numbers, -4, 3));
        assertTrue(ShopFilter.page(List.of(), 0, 3).isEmpty());
    }
}
