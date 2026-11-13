package assos.hytale.shop.shop;

import assos.hytale.shop.storage.JsonFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The items the admin shop trades. Three sources are merged into one list sorted by item id:
 * <ul>
 *   <li>automatic entries, every block of the game with a computed price ({@link #setAutoEntries});</li>
 *   <li>overrides, set by admins, which replace the automatic price or add any other item;</li>
 *   <li>exclusions, automatic items an admin removed from the shop.</li>
 * </ul>
 * Only overrides and exclusions are persisted, in {@code shop.json}.
 */
public final class ShopCatalog {

    private static final Logger LOGGER = Logger.getLogger(ShopCatalog.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 2;

    private final Path file;
    private final Map<String, ShopEntry> overrides = new LinkedHashMap<>();
    private final Set<String> excluded = new TreeSet<>();
    private Map<String, ShopEntry> autoEntries = Map.of();
    private Function<String, ShopCategory> categorizer = itemId -> ShopCategory.OTHER;
    private boolean writable = true;

    private List<ShopEntry> merged = List.of();
    private Map<String, ShopEntry> byId = Map.of();
    private Map<String, ShopEntry> byLowerId = Map.of();
    private Map<String, ShopCategory> categories = Map.of();

    public ShopCatalog(@Nonnull Path file) {
        this.file = file;
    }

    /**
     * Replaces the automatic entries, typically every block of the loaded game assets.
     */
    public synchronized void setAutoEntries(@Nonnull Collection<ShopEntry> entries) {
        Map<String, ShopEntry> auto = new HashMap<>();
        entries.forEach(entry -> auto.put(entry.itemId(), entry));
        this.autoEntries = auto;
        rebuild();
    }

    public synchronized void setCategorizer(@Nonnull Function<String, ShopCategory> categorizer) {
        this.categorizer = categorizer;
        rebuild();
    }

    /**
     * On failure the current items are kept and edits are refused until a load succeeds, so a broken file is
     * never overwritten.
     */
    public synchronized void load() throws IOException {
        try {
            loadFromDisk();
            writable = true;
        } catch (IOException | RuntimeException exception) {
            writable = false;
            throw exception;
        }
        rebuild();
    }

    private void loadFromDisk() throws IOException {
        if (!Files.exists(file)) {
            overrides.clear();
            excluded.clear();
            JsonFiles.writeAtomically(file, GSON, toStored());
            return;
        }

        Map<String, ShopEntry> loadedOverrides = new LinkedHashMap<>();
        Set<String> loadedExcluded = new TreeSet<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StoredCatalog stored = GSON.fromJson(reader, StoredCatalog.class);
            if (stored != null && stored.version > CURRENT_VERSION) {
                throw new IOException("Unsupported shop data version " + stored.version);
            }
            if (stored != null && stored.items != null) {
                for (StoredEntry raw : stored.items) {
                    if (raw == null) {
                        continue;
                    }
                    try {
                        ShopEntry entry = new ShopEntry(raw.itemId, raw.buyPrice, raw.sellPrice);
                        loadedOverrides.put(entry.itemId(), entry);
                    } catch (IllegalArgumentException exception) {
                        LOGGER.log(Level.WARNING, "Skipping invalid shop entry '" + raw.itemId + "': "
                                + exception.getMessage());
                    }
                }
            }
            if (stored != null && stored.excluded != null) {
                stored.excluded.stream()
                        .filter(id -> id != null && !id.isBlank())
                        .forEach(loadedExcluded::add);
            }
        } catch (JsonParseException exception) {
            throw new IOException("Could not read " + file, exception);
        }

        overrides.clear();
        overrides.putAll(loadedOverrides);
        excluded.clear();
        excluded.addAll(loadedExcluded);
    }

    /**
     * Every item of the shop, sorted by item id.
     */
    @Nonnull
    public synchronized List<ShopEntry> list() {
        return merged;
    }

    @Nonnull
    public synchronized ShopCategory categoryOf(@Nonnull String itemId) {
        return categories.getOrDefault(itemId, ShopCategory.OTHER);
    }

    /**
     * Exact match first, then case-insensitive, so {@code /shop buy rock_stone} still works.
     */
    @Nonnull
    public synchronized Optional<ShopEntry> find(@Nonnull String itemId) {
        ShopEntry exact = byId.get(itemId);
        if (exact != null) {
            return Optional.of(exact);
        }
        return Optional.ofNullable(byLowerId.get(itemId.toLowerCase(Locale.ROOT)));
    }

    public synchronized boolean isOverridden(@Nonnull String itemId) {
        return overrides.containsKey(itemId);
    }

    /**
     * Sets custom prices for an item, adding it to the shop if needed.
     *
     * @return {@code true} when the item was not in the shop before
     */
    public synchronized boolean put(@Nonnull ShopEntry entry) throws IOException {
        boolean created = !byId.containsKey(entry.itemId());
        Map<String, ShopEntry> previousOverrides = new LinkedHashMap<>(overrides);
        Set<String> previousExcluded = new TreeSet<>(excluded);
        overrides.put(entry.itemId(), entry);
        excluded.remove(entry.itemId());
        persistOrRollback(previousOverrides, previousExcluded);
        return created;
    }

    /**
     * Removes an item from the shop. Automatic items are remembered as excluded so they do not come back.
     */
    public synchronized boolean remove(@Nonnull String itemId) throws IOException {
        Optional<ShopEntry> existing = find(itemId);
        if (existing.isEmpty()) {
            return false;
        }
        String id = existing.get().itemId();
        Map<String, ShopEntry> previousOverrides = new LinkedHashMap<>(overrides);
        Set<String> previousExcluded = new TreeSet<>(excluded);
        overrides.remove(id);
        if (autoEntries.containsKey(id)) {
            excluded.add(id);
        }
        persistOrRollback(previousOverrides, previousExcluded);
        return true;
    }

    /**
     * Drops the custom prices and the exclusion of an item, so it goes back to its automatic price, or leaves
     * the shop if it is not a block.
     *
     * @return {@code false} when there was nothing to reset
     */
    public synchronized boolean reset(@Nonnull String itemId) throws IOException {
        String id = resolveStoredId(itemId);
        if (id == null) {
            return false;
        }
        Map<String, ShopEntry> previousOverrides = new LinkedHashMap<>(overrides);
        Set<String> previousExcluded = new TreeSet<>(excluded);
        overrides.remove(id);
        excluded.remove(id);
        persistOrRollback(previousOverrides, previousExcluded);
        return true;
    }

    private String resolveStoredId(String itemId) {
        for (String id : overrides.keySet()) {
            if (id.equalsIgnoreCase(itemId)) {
                return id;
            }
        }
        for (String id : excluded) {
            if (id.equalsIgnoreCase(itemId)) {
                return id;
            }
        }
        return null;
    }

    private void persistOrRollback(Map<String, ShopEntry> previousOverrides, Set<String> previousExcluded)
            throws IOException {
        if (!writable) {
            restore(previousOverrides, previousExcluded);
            throw new IOException("shop.json was not loaded; fix it and run /shopadmin reload");
        }
        try {
            JsonFiles.writeAtomically(file, GSON, toStored());
        } catch (IOException | RuntimeException exception) {
            restore(previousOverrides, previousExcluded);
            throw exception;
        }
        rebuild();
    }

    private void restore(Map<String, ShopEntry> previousOverrides, Set<String> previousExcluded) {
        overrides.clear();
        overrides.putAll(previousOverrides);
        excluded.clear();
        excluded.addAll(previousExcluded);
    }

    private void rebuild() {
        Map<String, ShopEntry> all = new HashMap<>(autoEntries);
        excluded.forEach(all::remove);
        all.putAll(overrides);

        List<ShopEntry> sorted = new ArrayList<>(all.values());
        sorted.sort(Comparator.comparing(ShopEntry::itemId, String.CASE_INSENSITIVE_ORDER));

        Map<String, ShopEntry> lower = new HashMap<>();
        Map<String, ShopCategory> categoryById = new HashMap<>();
        for (ShopEntry entry : sorted) {
            lower.putIfAbsent(entry.itemId().toLowerCase(Locale.ROOT), entry);
            ShopCategory category = categorizer.apply(entry.itemId());
            categoryById.put(entry.itemId(), category == null ? ShopCategory.OTHER : category);
        }

        this.merged = List.copyOf(sorted);
        this.byId = all;
        this.byLowerId = lower;
        this.categories = categoryById;
    }

    private StoredCatalog toStored() {
        StoredCatalog stored = new StoredCatalog();
        List<StoredEntry> items = new ArrayList<>();
        overrides.values().forEach(entry -> {
            StoredEntry raw = new StoredEntry();
            raw.itemId = entry.itemId();
            raw.buyPrice = entry.buyPrice();
            raw.sellPrice = entry.sellPrice();
            items.add(raw);
        });
        stored.items = items;
        stored.excluded = new ArrayList<>(excluded);
        return stored;
    }

    private static final class StoredCatalog {
        private int version = CURRENT_VERSION;
        private List<StoredEntry> items = new ArrayList<>();
        private List<String> excluded = new ArrayList<>();
    }

    private static final class StoredEntry {
        private String itemId;
        private long buyPrice;
        private long sellPrice;
    }
}
