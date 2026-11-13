package assos.hytale.shop.shop;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shop tabs, derived from the game's item categories ({@code Blocks.Rocks}, {@code Furniture.Doors}...).
 * {@link #basePrice} is the automatic buy price of a common item of that category.
 */
public enum ShopCategory {
    ROCKS("Rocks", "Blocks.Rocks", 3),
    SOILS("Soils", "Blocks.Soils", 2),
    WOOD("Wood", "Blocks.Wood", 4),
    PLANTS("Plants", "Blocks.Plants", 3),
    METAL("Metal", "Blocks.Metal", 15),
    CLOTH("Cloth", "Blocks.Cloth", 5),
    DECO("Deco", "Blocks.Deco", 8),
    ORES("Ores", "Blocks.Ores", 20),
    FURNITURE("Furniture", "Furniture.", 25),
    OTHER("Other", null, 10);

    private final String label;
    private final String gameCategory;
    private final long basePrice;

    ShopCategory(String label, String gameCategory, long basePrice) {
        this.label = label;
        this.gameCategory = gameCategory;
        this.basePrice = basePrice;
    }

    @Nonnull
    public String getLabel() {
        return label;
    }

    public long getBasePrice() {
        return basePrice;
    }

    /**
     * The first game category that maps to a tab wins; anything else (food, fluids, portals...) is {@link #OTHER}.
     */
    @Nonnull
    public static ShopCategory fromGameCategories(@Nullable String[] categories) {
        if (categories == null) {
            return OTHER;
        }
        for (String category : categories) {
            if (category == null) {
                continue;
            }
            for (ShopCategory candidate : values()) {
                if (candidate.gameCategory == null) {
                    continue;
                }
                boolean prefix = candidate.gameCategory.endsWith(".");
                if (prefix ? category.startsWith(candidate.gameCategory) : category.equals(candidate.gameCategory)) {
                    return candidate;
                }
            }
        }
        return OTHER;
    }

    @Nullable
    public static ShopCategory fromLabel(@Nullable String label) {
        for (ShopCategory category : values()) {
            if (category.label.equalsIgnoreCase(label)) {
                return category;
            }
        }
        return null;
    }
}
