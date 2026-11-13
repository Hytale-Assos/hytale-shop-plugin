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

    @Test
    void buyPriceDoublesPerQualityStep() {
        assertEquals(15, AutoPricing.buyPrice(ShopCategory.ROCKS, 0));
        assertEquals(15, AutoPricing.buyPrice(ShopCategory.ROCKS, 1));
        assertEquals(30, AutoPricing.buyPrice(ShopCategory.ROCKS, 2));
        assertEquals(240, AutoPricing.buyPrice(ShopCategory.ROCKS, 5));
        assertEquals(240, AutoPricing.buyPrice(ShopCategory.ROCKS, 10));
        assertEquals(125, AutoPricing.buyPrice(ShopCategory.FURNITURE, 1));
    }

    @Test
    void sellPriceIsAShareOfTheBuyPrice() {
        assertEquals(25, AutoPricing.sellPrice(100, 25));
        assertEquals(1, AutoPricing.sellPrice(2, 25));
        assertEquals(0, AutoPricing.sellPrice(100, 0));
        assertEquals(100, AutoPricing.sellPrice(100, 150));
        assertEquals(0, AutoPricing.sellPrice(1, -5));
    }
}
