package assos.hytale.shop.shop;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/** In-memory inventory with a fixed number of item units it can hold. */
final class FakeInventory implements PlayerInventory {

    private final Map<String, Integer> items = new HashMap<>();
    private final int capacity;
    boolean failNextAdd;

    FakeInventory(int capacity) {
        this.capacity = capacity;
    }

    FakeInventory with(String itemId, int quantity) {
        items.merge(itemId, quantity, Integer::sum);
        return this;
    }

    private int total() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public int count(@Nonnull String itemId) {
        return items.getOrDefault(itemId, 0);
    }

    @Override
    public boolean canAdd(@Nonnull String itemId, int quantity) {
        return quantity > 0 && total() + quantity <= capacity;
    }

    @Override
    public boolean add(@Nonnull String itemId, int quantity) {
        if (failNextAdd) {
            failNextAdd = false;
            return false;
        }
        if (!canAdd(itemId, quantity)) {
            return false;
        }
        items.merge(itemId, quantity, Integer::sum);
        return true;
    }

    @Override
    public boolean remove(@Nonnull String itemId, int quantity) {
        if (quantity < 1 || count(itemId) < quantity) {
            return false;
        }
        items.merge(itemId, -quantity, Integer::sum);
        return true;
    }
}
