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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The items the admin shop trades, persisted in {@code shop.json}. When the file does not exist yet the catalog
 * starts from {@link #DEFAULT_ENTRIES} and writes it, so admins have a file to edit.
 */
public final class ShopCatalog {

    public static final List<ShopEntry> DEFAULT_ENTRIES = List.of(
            new ShopEntry("Soil_Dirt", 2, 1),
            new ShopEntry("Soil_Grass", 3, 1),
            new ShopEntry("Soil_Sand", 2, 1),
            new ShopEntry("Soil_Gravel", 2, 1),
            new ShopEntry("Soil_Clay", 4, 2),
            new ShopEntry("Rock_Stone", 3, 1),
            new ShopEntry("Rock_Stone_Cobble", 2, 1),
            new ShopEntry("Rock_Stone_Brick", 6, 2),
            new ShopEntry("Rock_Sandstone", 4, 2),
            new ShopEntry("Rock_Marble", 10, 4),
            new ShopEntry("Wood_Oak_Trunk", 5, 2));

    private static final Logger LOGGER = Logger.getLogger(ShopCatalog.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 1;

    private final Path file;
    private final Map<String, ShopEntry> entries = new LinkedHashMap<>();
    private boolean writable = true;

    public ShopCatalog(@Nonnull Path file) {
        this.file = file;
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
    }

    private void loadFromDisk() throws IOException {
        if (!Files.exists(file)) {
            entries.clear();
            DEFAULT_ENTRIES.forEach(entry -> entries.put(entry.itemId(), entry));
            JsonFiles.writeAtomically(file, GSON, toStored());
            return;
        }

        Map<String, ShopEntry> loaded = new LinkedHashMap<>();
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
                        loaded.put(entry.itemId(), entry);
                    } catch (IllegalArgumentException exception) {
                        LOGGER.log(Level.WARNING, "Skipping invalid shop entry '" + raw.itemId + "': "
                                + exception.getMessage());
                    }
                }
            }
        } catch (JsonParseException exception) {
            throw new IOException("Could not read " + file, exception);
        }

        entries.clear();
        entries.putAll(loaded);
    }

    @Nonnull
    public synchronized List<ShopEntry> list() {
        return List.copyOf(entries.values());
    }

    /**
     * Exact match first, then case-insensitive, so {@code /shop buy rock_stone} still works.
     */
    @Nonnull
    public synchronized Optional<ShopEntry> find(@Nonnull String itemId) {
        ShopEntry exact = entries.get(itemId);
        if (exact != null) {
            return Optional.of(exact);
        }
        return entries.values().stream()
                .filter(entry -> entry.itemId().equalsIgnoreCase(itemId))
                .findFirst();
    }

    /**
     * Adds the entry, or replaces the prices of an existing one while keeping its position.
     *
     * @return {@code true} when the item was new
     */
    public synchronized boolean put(@Nonnull ShopEntry entry) throws IOException {
        Map<String, ShopEntry> snapshot = new LinkedHashMap<>(entries);
        boolean created = !entries.containsKey(entry.itemId());
        entries.put(entry.itemId(), entry);
        persistOrRollback(snapshot);
        return created;
    }

    public synchronized boolean remove(@Nonnull String itemId) throws IOException {
        Optional<ShopEntry> existing = find(itemId);
        if (existing.isEmpty()) {
            return false;
        }
        Map<String, ShopEntry> snapshot = new LinkedHashMap<>(entries);
        entries.remove(existing.get().itemId());
        persistOrRollback(snapshot);
        return true;
    }

    private void persistOrRollback(Map<String, ShopEntry> snapshot) throws IOException {
        if (!writable) {
            entries.clear();
            entries.putAll(snapshot);
            throw new IOException("shop.json was not loaded; fix it and run /shopadmin reload");
        }
        try {
            JsonFiles.writeAtomically(file, GSON, toStored());
        } catch (IOException | RuntimeException exception) {
            entries.clear();
            entries.putAll(snapshot);
            throw exception;
        }
    }

    private StoredCatalog toStored() {
        StoredCatalog stored = new StoredCatalog();
        List<StoredEntry> items = new ArrayList<>();
        entries.values().forEach(entry -> {
            StoredEntry raw = new StoredEntry();
            raw.itemId = entry.itemId();
            raw.buyPrice = entry.buyPrice();
            raw.sellPrice = entry.sellPrice();
            items.add(raw);
        });
        stored.items = items;
        return stored;
    }

    private static final class StoredCatalog {
        private int version = CURRENT_VERSION;
        private List<StoredEntry> items = new ArrayList<>();
    }

    private static final class StoredEntry {
        private String itemId;
        private long buyPrice;
        private long sellPrice;
    }
}
