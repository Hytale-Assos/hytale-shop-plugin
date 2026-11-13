package assos.hytale.shop.shop;

import javax.annotation.Nonnull;

/**
 * The part of a player's inventory the shop needs. Implementations must be all-or-nothing: {@link #add} and
 * {@link #remove} either move the full quantity or change nothing.
 */
public interface PlayerInventory {

    int count(@Nonnull String itemId);

    boolean canAdd(@Nonnull String itemId, int quantity);

    boolean add(@Nonnull String itemId, int quantity);

    boolean remove(@Nonnull String itemId, int quantity);
}
