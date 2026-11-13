package assos.hytale.shop.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PricingAndCategoryTest {

    @Test
    void gameCategoriesMapToTabs() {
        assertEquals(ShopCategory.ROCKS, ShopCategory.fromGameCategories(new String[] {"Blocks.Rocks"}));
        assertEquals(ShopCategory.FURNITURE, ShopCategory.fromGameCategories(new String[] {"Furniture.Doors"}));
        assertEquals(ShopCategory.PLANTS,
                ShopCategory.fromGameCategories(new String[] {"Items.Foods", "Blocks.Plants"}));
    }

    @Test
    void unknownOrMissingCategoriesAreOther() {
        assertEquals(ShopCategory.OTHER, ShopCategory.fromGameCategories(null));
        assertEquals(ShopCategory.OTHER, ShopCategory.fromGameCategories(new String[0]));
        assertEquals(ShopCategory.OTHER, ShopCategory.fromGameCategories(new String[] {null, "Blocks.Fluids"}));
        assertEquals(ShopCategory.OTHER, ShopCategory.fromGameCategories(new String[] {"Blocks.RocksExtra"}));
    }

    @Test
    void tabsAreFoundByLabelIgnoringCase() {
        assertEquals(ShopCategory.WOOD, ShopCategory.fromLabel("wood"));
        assertNull(ShopCategory.fromLabel("All"));
        assertNull(ShopCategory.fromLabel(null));
    }
}
