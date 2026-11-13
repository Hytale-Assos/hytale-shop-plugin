package assos.hytale.shop.economy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Opens the account with the starting balance if needed and records the latest known username.
     */
    public synchronized void touch(@Nonnull UUID playerId, @Nullable String name) throws IOException {
        Account existing = accounts.get(playerId);
        if (existing != null && (name == null || name.equals(existing.getName()))) {
            return;
        }
        Map<UUID, Account> snapshot = snapshot();
        if (existing == null) {
            accounts.put(playerId, new Account(name, startingBalance));
        } else {
            existing.setName(name);
        }
        persistOrRollback(snapshot);
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

    /**
     * Removes up to {@code amount}, never going below zero.
     *
     * @return the amount actually removed
     */
    public synchronized long take(@Nonnull UUID playerId, long amount) throws IOException {
        requirePositive(amount);
        Map<UUID, Account> snapshot = snapshot();
        Account account = account(playerId);
        long removed = Math.min(amount, account.getBalance());
        account.setBalance(account.getBalance() - removed);
        persistOrRollback(snapshot);
        return removed;
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

    @Nonnull
    public synchronized List<TopEntry> top(int limit) {
        List<TopEntry> entries = new ArrayList<>();
        accounts.forEach((playerId, account) -> entries.add(
                new TopEntry(playerId, account.getName(), account.getBalance())));
        entries.sort(Comparator.comparingLong(TopEntry::balance).reversed()
                .thenComparing(entry -> entry.name() == null ? "" : entry.name()));
        return entries.size() > limit ? List.copyOf(entries.subList(0, Math.max(0, limit))) : entries;
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

    public record TopEntry(@Nonnull UUID playerId, @Nullable String name, long balance) {
    }
}
