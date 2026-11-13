package assos.hytale.shop.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import assos.hytale.shop.economy.EconomyService.TopEntry;
import assos.hytale.shop.economy.EconomyService.TransferStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EconomyServiceTest {

    @TempDir
    Path tempDir;

    private final UUID alice = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID bob = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private EconomyService service() {
        return new EconomyService(new AccountStorage(tempDir.resolve("accounts.json")), 100);
    }

    /** A storage whose target is a non-empty directory, so every save fails. */
    private EconomyService brokenService() throws IOException {
        Path target = tempDir.resolve("broken.json");
        Files.createDirectories(target);
        Files.writeString(target.resolve("blocker"), "x");
        return new EconomyService(new AccountStorage(target), 100);
    }

    @Test
    void unknownPlayerHasTheStartingBalanceWithoutAnAccount() {
        EconomyService economy = service();

        assertEquals(100, economy.getBalance(alice));
        assertFalse(economy.hasAccount(alice));
    }

    @Test
    void negativeStartingBalanceIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new EconomyService(new AccountStorage(tempDir.resolve("a.json")), -1));
    }

    @Test
    void touchOpensTheAccountAndRecordsTheName() throws Exception {
        EconomyService economy = service();

        economy.touch(alice, "Alice");
        economy.touch(alice, "AliceRenamed");

        assertTrue(economy.hasAccount(alice));
        assertEquals(100, economy.getBalance(alice));
        assertEquals("AliceRenamed", economy.top(10).get(0).name());
    }

    @Test
    void depositAddsToTheBalance() throws Exception {
        EconomyService economy = service();

        assertEquals(150, economy.deposit(alice, 50));
        assertEquals(150, economy.getBalance(alice));
    }

    @Test
    void depositOverflowIsRejectedAndLeavesTheBalanceUnchanged() throws Exception {
        EconomyService economy = service();
        economy.setBalance(alice, Long.MAX_VALUE - 1);

        assertThrows(IllegalArgumentException.class, () -> economy.deposit(alice, 5));
        assertEquals(Long.MAX_VALUE - 1, economy.getBalance(alice));
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        EconomyService economy = service();

        assertThrows(IllegalArgumentException.class, () -> economy.deposit(alice, 0));
        assertThrows(IllegalArgumentException.class, () -> economy.withdraw(alice, -5));
        assertThrows(IllegalArgumentException.class, () -> economy.take(alice, 0));
        assertThrows(IllegalArgumentException.class, () -> economy.transfer(alice, bob, 0));
        assertThrows(IllegalArgumentException.class, () -> economy.setBalance(alice, -1));
    }

    @Test
    void withdrawSucceedsUpToTheExactBalance() throws Exception {
        EconomyService economy = service();

        assertTrue(economy.withdraw(alice, 100));
        assertEquals(0, economy.getBalance(alice));
    }

    @Test
    void withdrawRefusesWhenTheBalanceIsTooLow() throws Exception {
        EconomyService economy = service();

        assertFalse(economy.withdraw(alice, 101));
        assertEquals(100, economy.getBalance(alice));
        assertFalse(economy.hasAccount(alice));
    }

    @Test
    void takeNeverGoesBelowZero() throws Exception {
        EconomyService economy = service();

        assertEquals(100, economy.take(alice, 500));
        assertEquals(0, economy.getBalance(alice));
    }

    @Test
    void setBalanceAcceptsZero() throws Exception {
        EconomyService economy = service();

        economy.setBalance(alice, 0);

        assertEquals(0, economy.getBalance(alice));
    }

    @Test
    void transferMovesTheAmountBetweenAccounts() throws Exception {
        EconomyService economy = service();

        assertEquals(TransferStatus.SUCCESS, economy.transfer(alice, bob, 40));
        assertEquals(60, economy.getBalance(alice));
        assertEquals(140, economy.getBalance(bob));
    }

    @Test
    void transferWithInsufficientFundsChangesNothing() throws Exception {
        EconomyService economy = service();

        assertEquals(TransferStatus.INSUFFICIENT_FUNDS, economy.transfer(alice, bob, 101));
        assertEquals(100, economy.getBalance(alice));
        assertEquals(100, economy.getBalance(bob));
    }

    @Test
    void transferToYourselfIsRefused() throws Exception {
        EconomyService economy = service();

        assertEquals(TransferStatus.SAME_ACCOUNT, economy.transfer(alice, alice, 10));
        assertEquals(100, economy.getBalance(alice));
    }

    @Test
    void balancesSurviveAReload() throws Exception {
        EconomyService writer = service();
        writer.touch(alice, "Alice");
        writer.deposit(alice, 25);
        writer.transfer(alice, bob, 5);

        EconomyService reader = service();
        reader.load();

        assertEquals(120, reader.getBalance(alice));
        assertEquals(105, reader.getBalance(bob));
        assertEquals("Alice", reader.top(10).stream().filter(entry -> entry.playerId().equals(alice))
                .findFirst().orElseThrow().name());
    }

    @Test
    void failedSaveRollsBackTheMutation() throws Exception {
        EconomyService economy = brokenService();

        assertThrows(IOException.class, () -> economy.deposit(alice, 50));
        assertEquals(100, economy.getBalance(alice));
        assertFalse(economy.hasAccount(alice));

        assertThrows(IOException.class, () -> economy.transfer(alice, bob, 10));
        assertEquals(100, economy.getBalance(alice));
        assertEquals(100, economy.getBalance(bob));
    }

    @Test
    void failedLoadRefusesWritesSoTheFileIsNotOverwritten() throws Exception {
        Path file = tempDir.resolve("accounts.json");
        Files.writeString(file, "{ not json");
        EconomyService economy = new EconomyService(new AccountStorage(file), 100);

        assertThrows(IOException.class, economy::load);
        assertThrows(IOException.class, () -> economy.deposit(alice, 10));

        assertEquals("{ not json", Files.readString(file));
        assertEquals(100, economy.getBalance(alice));
    }

    @Test
    void successfulReloadAfterAFailedLoadAllowsWritesAgain() throws Exception {
        Path file = tempDir.resolve("accounts.json");
        Files.writeString(file, "{ not json");
        EconomyService economy = new EconomyService(new AccountStorage(file), 100);
        assertThrows(IOException.class, economy::load);

        Files.delete(file);
        economy.load();

        assertEquals(110, economy.deposit(alice, 10));
    }

    @Test
    void topIsSortedByBalanceThenNameAndLimited() throws Exception {
        EconomyService economy = service();
        UUID carol = UUID.randomUUID();
        economy.touch(alice, "Alice");
        economy.touch(bob, "Bob");
        economy.touch(carol, "Carol");
        economy.deposit(bob, 50);

        List<TopEntry> top = economy.top(2);

        assertEquals(2, top.size());
        assertEquals("Bob", top.get(0).name());
        assertEquals(150, top.get(0).balance());
        assertEquals("Alice", top.get(1).name());
    }

    @Test
    void topWithZeroLimitIsEmpty() throws Exception {
        EconomyService economy = service();
        economy.touch(alice, "Alice");

        assertTrue(economy.top(0).isEmpty());
    }
}
