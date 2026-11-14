package rush.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The list of items customers can choose from. */
public class Menu {

    private final List<MenuItem> items = new ArrayList<>();

    /** Builds the base-setting menu. */
    public static Menu createDefault() {
        Menu menu = new Menu();
        menu.add(new MenuItem("Salad", 900, 1));
        menu.add(new MenuItem("Burger", 1500, 1));
        menu.add(new MenuItem("Pasta", 1800, 2));
        return menu;
    }

    public void add(MenuItem item) {
        items.add(item);
    }

    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Documented deterministic choice rule: the customer with arrival number n
     * orders menu item number ((n - 1) mod menu size), i.e. customers cycle
     * through the menu in the order it is printed.
     */
    public MenuItem itemForArrival(int arrivalNumber) {
        return items.get((arrivalNumber - 1) % items.size());
    }

    /** Finds an item by name, or throws if the menu does not contain it. */
    public MenuItem findByName(String name) {
        for (MenuItem item : items) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        throw new IllegalArgumentException("No menu item called " + name);
    }
}
