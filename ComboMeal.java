package rush.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A combo of two menu items. It is a MenuItem, so the Waiter, Chef and Order
 * treat it like any other dish, and it also holds its two items (composition).
 * Preparation units = both items added together. Price = 90% of their total
 * price, rounded to the nearest cent.
 */
public class ComboMeal extends MenuItem {

    private static final int PERCENT_PAYABLE = 90;

    private final List<MenuItem> components;

    public ComboMeal(MenuItem first, MenuItem second) {
        super(first.getName() + " + " + second.getName() + " Combo",
                Money.percentOf(first.getPriceCents() + second.getPriceCents(), PERCENT_PAYABLE),
                first.getPrepUnits() + second.getPrepUnits());
        this.components = Collections.unmodifiableList(Arrays.asList(first, second));
    }

    public List<MenuItem> getComponents() {
        return components;
    }
}
