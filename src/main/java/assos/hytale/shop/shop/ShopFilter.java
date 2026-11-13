package assos.hytale.shop.shop;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Category, search and paging over the shop list, shared by the window and {@code /shop list}.
 */
public final class ShopFilter {

    private ShopFilter() {
    }

    /**
     * @param category {@code null} for every category
     * @param search   words matched against the item id, ignoring case, {@code _} and spaces; every word must
     *                 match ({@code "oak plank"} finds {@code Wood_Oak_Planks})
     */
    @Nonnull
    public static List<ShopEntry> filter(@Nonnull List<ShopEntry> entries,
            @Nonnull Function<String, ShopCategory> categoryOf, @Nullable ShopCategory category,
            @Nullable String search) {
        String[] words = search == null || search.isBlank()
                ? new String[0]
                : search.trim().toLowerCase(Locale.ROOT).split("[\\s_]+");
        return entries.stream()
                .filter(entry -> category == null || categoryOf.apply(entry.itemId()) == category)
                .filter(entry -> matches(entry.itemId(), words))
                .toList();
    }

    public static int pageCount(int size, int pageSize) {
        return Math.max(1, (size + pageSize - 1) / pageSize);
    }

    /**
     * The entries of a zero-based page, clamped to the valid range.
     */
    @Nonnull
    public static <T> List<T> page(@Nonnull List<T> entries, int page, int pageSize) {
        int last = pageCount(entries.size(), pageSize) - 1;
        int index = Math.min(Math.max(page, 0), last);
        int from = Math.min(index * pageSize, entries.size());
        int to = Math.min(from + pageSize, entries.size());
        return entries.subList(from, to);
    }

    private static boolean matches(String itemId, String[] words) {
        String id = itemId.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (!word.isEmpty() && !id.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
