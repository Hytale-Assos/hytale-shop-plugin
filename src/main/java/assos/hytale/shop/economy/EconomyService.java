package assos.hytale.shop.economy;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hcoin balances. Every mutation is persisted before it returns; if the save fails the in-memory state is
 * rolled back and the {@link IOException} is rethrown, so memory and disk never disagree.
 */
public final class EconomyService {

    private final AccountStorage storage;
    private final long startingBalance;
    private final Map<UUID, Account> accounts = new HashMap<>();

    public EconomyService(@Nonnull AccountStorage storage, long startingBalance) {
        if (startingBalance < 0) {
            throw new IllegalArgumentException("Starting balance cannot be negative");
        }
        this.storage = storage;
        this.startingBalance = startingBalance;
    }

    public synchronized void load() throws IOException {
        Map<UUID, Account> loaded = storage.load();
        accounts.clear();
        accounts.putAll(loaded);
    }

    public long getStartingBalance() {
        return startingBalance;
    }

    public synchronized long getBalance(@Nonnull UUID playerId) {
        Account account = accounts.get(playerId);
        return account == null ? startingBalance : account.getBalance();
    }

    public synchronized boolean hasAccount(@Nonnull UUID playerId) {
        return accounts.containsKey(playerId);
    }

    public synchronized long deposit(@Nonnull UUID playerId, long amount) throws IOException {
        requirePositive(amount);
        Map<UUID, Account> snapshot = snapshot();
        Account account = account(playerId);
        account.setBalance(add(account.getBalance(), amount));
        persistOrRollback(snapshot);
        return account.getBalance();
    }

    /**
     * @return {@code false} without changing anything when the balance is too low
     */
    public synchronized boolean withdraw(@Nonnull UUID playerId, long amount) throws IOException {
        requirePositive(amount);
        if (getBalance(playerId) < amount) {
            return false;
        }
        Map<UUID, Account> snapshot = snapshot();
        Account account = account(playerId);
        account.setBalance(account.getBalance() - amount);
        persistOrRollback(snapshot);
        return true;
    }

    public synchronized void setBalance(@Nonnull UUID playerId, long amount) throws IOException {
        if (amount < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        Map<UUID, Account> snapshot = snapshot();
        account(playerId).setBalance(amount);
        persistOrRollback(snapshot);
    }

    @Nonnull
    public synchronized TransferStatus transfer(@Nonnull UUID from, @Nonnull UUID to, long amount)
            throws IOException {
        requirePositive(amount);
        if (from.equals(to)) {
            return TransferStatus.SAME_ACCOUNT;
        }
        if (getBalance(from) < amount) {
            return TransferStatus.INSUFFICIENT_FUNDS;
        }
        long receiverBalance = add(getBalance(to), amount);

        Map<UUID, Account> snapshot = snapshot();
        Account sender = account(from);
        sender.setBalance(sender.getBalance() - amount);
        account(to).setBalance(receiverBalance);
        persistOrRollback(snapshot);
        return TransferStatus.SUCCESS;
    }

    private Account account(UUID playerId) {
        return accounts.computeIfAbsent(playerId, ignored -> new Account(null, startingBalance));
    }

    private Map<UUID, Account> snapshot() {
        Map<UUID, Account> copy = new HashMap<>();
        accounts.forEach((id, account) -> copy.put(id, new Account(account.getName(), account.getBalance())));
        return copy;
    }

    private void persistOrRollback(Map<UUID, Account> snapshot) throws IOException {
        try {
            storage.save(accounts);
        } catch (IOException | RuntimeException exception) {
            accounts.clear();
            accounts.putAll(snapshot);
            throw exception;
        }
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private static long add(long balance, long amount) {
        try {
            return Math.addExact(balance, amount);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount is too large");
        }
    }

    public enum TransferStatus {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        SAME_ACCOUNT
    }

}
