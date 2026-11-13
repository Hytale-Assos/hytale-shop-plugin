package assos.hytale.shop.shop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShopEntryTest {

    @Test
    void zeroPriceDisablesThatDirection() {
        ShopEntry buyOnly = new ShopEntry("A", 5, 0);
        ShopEntry sellOnly = new ShopEntry("B", 0, 5);

        assertTrue(buyOnly.canBuy());
        assertFalse(buyOnly.canSell());
        assertFalse(sellOnly.canBuy());
        assertTrue(sellOnly.canSell());
    }

    @Test
    void invalidEntriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ShopEntry(null, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShopEntry(" ", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShopEntry("A", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ShopEntry("A", 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new ShopEntry("A", 0, 0));
    }
}
