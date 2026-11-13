package assos.hytale.shop.commands;

import assos.hytale.shop.shop.ShopCatalog;
import assos.hytale.shop.shop.ShopEntry;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionResult;
import com.hypixel.hytale.server.core.command.system.suggestion.SuggestionUtil;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public final class ShopSuggestions {

    private ShopSuggestions() {
    }

    public static void suggestItems(@Nonnull ShopCatalog catalog, @Nonnull Predicate<ShopEntry> filter,
            @Nonnull String input, @Nonnull SuggestionResult result) {
        SuggestionUtil.suggestFiltered(
                catalog.list().stream().filter(filter).map(ShopEntry::itemId).toList(), input, result);
    }
}
