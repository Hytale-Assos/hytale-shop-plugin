package assos.hytale.shop.economy;

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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON persistence for player accounts. Not thread-safe on its own: {@link EconomyService} serializes access.
 */
public final class AccountStorage {

    private static final Logger LOGGER = Logger.getLogger(AccountStorage.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 1;

    private final Path file;

    public AccountStorage(@Nonnull Path file) {
        this.file = file;
    }

    @Nonnull
    public Map<UUID, Account> load() throws IOException {
        Map<UUID, Account> accounts = new HashMap<>();
        if (!Files.exists(file)) {
            return accounts;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StoredAccounts stored = GSON.fromJson(reader, StoredAccounts.class);
            if (stored == null || stored.accounts == null) {
                return accounts;
            }
            if (stored.version > CURRENT_VERSION) {
                throw new IOException("Unsupported accounts data version " + stored.version);
            }
            stored.accounts.forEach((rawId, account) -> {
                UUID playerId;
                try {
                    playerId = UUID.fromString(rawId);
                } catch (IllegalArgumentException exception) {
                    LOGGER.log(Level.WARNING, "Skipping account with invalid player UUID '" + rawId + "'");
                    return;
                }
                if (account == null || account.getBalance() < 0) {
                    LOGGER.log(Level.WARNING, "Skipping invalid account for player " + playerId);
                    return;
                }
                accounts.put(playerId, account);
            });
        } catch (JsonParseException exception) {
            throw new IOException("Could not read " + file, exception);
        }
        return accounts;
    }

    public void save(@Nonnull Map<UUID, Account> accounts) throws IOException {
        StoredAccounts stored = new StoredAccounts();
        accounts.forEach((playerId, account) -> stored.accounts.put(playerId.toString(), account));
        JsonFiles.writeAtomically(file, GSON, stored);
    }

    private static final class StoredAccounts {
        private int version = CURRENT_VERSION;
        private Map<String, Account> accounts = new TreeMap<>();
    }
}
