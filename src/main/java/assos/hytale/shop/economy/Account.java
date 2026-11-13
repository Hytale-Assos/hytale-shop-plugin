package assos.hytale.shop.economy;

import javax.annotation.Nullable;

public final class Account {

    private String name;
    private long balance;

    public Account(@Nullable String name, long balance) {
        this.name = name;
        this.balance = balance;
    }

    @Nullable
    public String getName() {
        return name;
    }

    void setName(@Nullable String name) {
        this.name = name;
    }

    public long getBalance() {
        return balance;
    }

    void setBalance(long balance) {
        this.balance = balance;
    }
}
