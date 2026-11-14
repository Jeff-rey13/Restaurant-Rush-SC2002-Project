package rush.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The list of items customers can choose from. */
public class Menu {

    private final List<MenuItem> items = new ArrayList<>();

    /** Builds the base menu, plus the two combo meals (Stage 3). */
    public static Menu createDefault() {
        Menu menu = new Menu();
        MenuItem salad = new MenuItem("Salad", 900, 1);
        MenuItem burger = new MenuItem("Burger", 1500, 1);
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        menu.add(salad);
        menu.add(burger);
        menu.add(pasta);
        menu.add(new ComboMeal(salad, burger));   // $21.60, 2 units
        menu.add(new ComboMeal(salad, pasta));    // $24.30, 3 units
        return menu;
    }

    public void add(MenuItem item) {
        items.add(item);
    }

    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Fixed choice rule: the customer with arrival number n orders menu item
     * ((n - 1) mod menu size), so customers go through the menu in the order
     * it is printed.
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
