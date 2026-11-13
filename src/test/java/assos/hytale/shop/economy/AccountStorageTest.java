package assos.hytale.shop.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AccountStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileLoadsNoAccount() throws Exception {
        assertTrue(new AccountStorage(tempDir.resolve("accounts.json")).load().isEmpty());
    }

    @Test
    void saveThenLoadRoundTrips() throws Exception {
        Path file = tempDir.resolve("nested").resolve("accounts.json");
        UUID player = UUID.randomUUID();
        Map<UUID, Account> accounts = new HashMap<>();
        accounts.put(player, new Account("Alice", 42));

        new AccountStorage(file).save(accounts);
        Map<UUID, Account> loaded = new AccountStorage(file).load();

        assertEquals(1, loaded.size());
        assertEquals("Alice", loaded.get(player).getName());
        assertEquals(42, loaded.get(player).getBalance());
    }

    @Test
    void secondSaveKeepsABackupOfThePreviousFile() throws Exception {
        Path file = tempDir.resolve("accounts.json");
        AccountStorage storage = new AccountStorage(file);
        UUID player = UUID.randomUUID();

        storage.save(Map.of(player, new Account("Alice", 1)));
        storage.save(Map.of(player, new Account("Alice", 2)));

        Path backup = tempDir.resolve("accounts.json.bak");
        assertTrue(Files.exists(backup));
        assertTrue(Files.readString(backup).contains("\"balance\": 1"));
        assertFalse(Files.exists(tempDir.resolve("accounts.json.tmp")));
    }

    @Test
    void invalidEntriesAreSkipped() throws Exception {
        Path file = tempDir.resolve("accounts.json");
        String valid = UUID.randomUUID().toString();
        Files.writeString(file, """
                {
                  "version": 1,
                  "accounts": {
                    "not-a-uuid": { "name": "Ghost", "balance": 10 },
                    "%s": { "name": "Alice", "balance": 10 },
                    "%s": { "name": "Negative", "balance": -5 },
                    "%s": null
                  }
                }
                """.formatted(valid, UUID.randomUUID(), UUID.randomUUID()));

        Map<UUID, Account> loaded = new AccountStorage(file).load();

        assertEquals(1, loaded.size());
        assertEquals("Alice", loaded.get(UUID.fromString(valid)).getName());
    }

    @Test
    void newerVersionIsRejected() throws Exception {
        Path file = tempDir.resolve("accounts.json");
        Files.writeString(file, "{ \"version\": 99, \"accounts\": {} }");

        assertThrows(IOException.class, () -> new AccountStorage(file).load());
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        Path file = tempDir.resolve("accounts.json");
        Files.writeString(file, "{ \"accounts\": [");

        assertThrows(IOException.class, () -> new AccountStorage(file).load());
    }

    @Test
    void emptyFileLoadsNoAccount() throws Exception {
        Path file = tempDir.resolve("accounts.json");
        Files.writeString(file, "");

        assertTrue(new AccountStorage(file).load().isEmpty());
    }
}
