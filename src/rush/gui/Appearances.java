package rush.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

import rush.domain.Customer;

/**
 * Chooses how each character looks. Customer looks are registered by the
 * customer's type name, so a new customer type only needs one registration
 * line here and no change to the drawing code.
 */
final class Appearances {

    private static final Color[] SKINS = {
        new Color(244, 205, 170), new Color(198, 140, 100), new Color(232, 182, 140),
        new Color(141, 94, 62), new Color(250, 218, 190), new Color(176, 118, 80)
    };
    private static final Color[] HAIRS = {
        new Color(52, 36, 28), new Color(196, 140, 60), new Color(30, 28, 30),
        new Color(140, 70, 40), new Color(90, 60, 40), new Color(220, 200, 150)
    };
    private static final Color[] OUTFITS = {
        new Color(76, 130, 190), new Color(98, 160, 110), new Color(200, 120, 60),
        new Color(150, 90, 170), new Color(70, 150, 160), new Color(200, 170, 70)
    };
    private static final Look.Hair[] STYLES = {
        Look.Hair.LONG, Look.Hair.SHORT, Look.Hair.BUN, Look.Hair.CURLY, Look.Hair.SPIKY, Look.Hair.BALD
    };
    private static final String[] NAMES = {
        "Ana", "Ben", "Chloe", "Dev", "Elena", "Farid", "Grace", "Hiro", "Ines", "Jun"
    };

    private static final Map<String, IntFunction<Look>> CUSTOMER_LOOKS = new HashMap<>();

    static {
        register("Regular", Appearances::casual);
    }

    private Appearances() {
    }

    static void register(String customerType, IntFunction<Look> look) {
        CUSTOMER_LOOKS.put(customerType, look);
    }

    static Look forCustomer(Customer customer) {
        IntFunction<Look> look = CUSTOMER_LOOKS.getOrDefault(customer.getTypeName(), Appearances::casual);
        return look.apply(customer.getNumber());
    }

    static String nameOf(Customer customer) {
        return NAMES[(customer.getNumber() - 1) % NAMES.length];
    }

    /** Everyday clothes; varied by arrival number so every customer looks different. */
    static Look casual(int n) {
        int i = n - 1;
        return new Look(SKINS[i % SKINS.length], HAIRS[(i * 2 + 1) % HAIRS.length], STYLES[i % STYLES.length],
                OUTFITS[i % OUTFITS.length], new Color(60, 64, 82), Look.Accessory.NONE, null);
    }

    static Look waiter() {
        return new Look(SKINS[2], HAIRS[0], Look.Hair.SHORT, new Color(245, 245, 240), new Color(40, 40, 46),
                Look.Accessory.WAITER_VEST, new Color(40, 40, 46));
    }

    static Look chef() {
        return new Look(SKINS[1], HAIRS[2], Look.Hair.SHORT, new Color(250, 250, 248), new Color(70, 70, 80),
                Look.Accessory.CHEF_HAT, new Color(200, 60, 50));
    }

    static Look cashier() {
        return new Look(SKINS[4], HAIRS[1], Look.Hair.BUN, new Color(60, 120, 90), new Color(50, 50, 60),
                Look.Accessory.CASHIER_CAP, new Color(60, 120, 90));
    }
}
