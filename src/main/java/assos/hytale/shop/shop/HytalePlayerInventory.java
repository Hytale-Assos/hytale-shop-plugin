package assos.hytale.shop.shop;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Hotbar, storage and backpack of a player. Must be used on the world thread of that player.
 */
public final class HytalePlayerInventory implements PlayerInventory {

    private final ItemContainer container;

    private HytalePlayerInventory(@Nonnull ItemContainer container) {
        this.container = container;
    }

    @Nullable
    public static HytalePlayerInventory of(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        if (!ref.isValid()) {
            return null;
        }
        ItemContainer container = InventoryComponent.getCombined(store, ref,
                InventoryComponent.HOTBAR_STORAGE_BACKPACK);
        return container == null ? null : new HytalePlayerInventory(container);
    }

    @Override
    public int count(@Nonnull String itemId) {
        int[] total = {0};
        container.forEach((slot, stack) -> {
            if (!ItemStack.isEmpty(stack) && itemId.equals(stack.getItemId())) {
                total[0] += stack.getQuantity();
            }
        });
        return total[0];
    }

    @Override
    public boolean canAdd(@Nonnull String itemId, int quantity) {
        List<ItemStack> stacks = split(itemId, quantity);
        return stacks != null && container.canAddItemStacks(stacks);
    }

    @Override
    public boolean add(@Nonnull String itemId, int quantity) {
        List<ItemStack> stacks = split(itemId, quantity);
        if (stacks == null || !container.canAddItemStacks(stacks)) {
            return false;
        }
        return container.addItemStacks(stacks, true, false, true).succeeded();
    }

    @Override
    public boolean remove(@Nonnull String itemId, int quantity) {
        if (quantity < 1) {
            return false;
        }
        ItemStack query = new ItemStack(itemId, quantity);
        if (!container.canRemoveItemStack(query)) {
            return false;
        }
        return container.removeItemStack(query, true, true).succeeded();
    }

    /**
     * Splits a quantity into stacks no larger than the item's max stack size.
     */
    @Nullable
    private static List<ItemStack> split(String itemId, int quantity) {
        Item item = Item.getAssetMap().getAsset(itemId);
        if (item == null || quantity < 1) {
            return null;
        }
        int maxStack = Math.max(1, item.getMaxStack());
        List<ItemStack> stacks = new ArrayList<>();
        int remaining = quantity;
        while (remaining > 0) {
            int size = Math.min(remaining, maxStack);
            stacks.add(new ItemStack(itemId, size));
            remaining -= size;
        }
        return stacks;
    }
}
