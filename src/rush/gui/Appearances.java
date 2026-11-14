package rush.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

import rush.domain.Customer;

/**
 * Chooses how each character looks. Customer styles (look, greeting and name
 * badge) are registered by the customer's type name, so a new customer type
 * only needs one registration line here and no change to the drawing code.
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

    /** How one customer type is presented. */
    static final class CustomerStyle {
        final IntFunction<Look> look;
        final String greeting;
        /** Colour of the type badge next to the name tag, or null for no badge. */
        final Color badge;

        CustomerStyle(IntFunction<Look> look, String greeting, Color badge) {
            this.look = look;
            this.greeting = greeting;
            this.badge = badge;
        }
    }

    private static final CustomerStyle DEFAULT = new CustomerStyle(Appearances::casual, "Hello! Table for one?", null);
    private static final Map<String, CustomerStyle> STYLES_BY_TYPE = new HashMap<>();

    static {
        register("Regular", DEFAULT);
        register("VIP", new CustomerStyle(Appearances::vip, "Good evening. Your best table, please.",
                new Color(214, 160, 30)));
        register("Critic", new CustomerStyle(Appearances::critic, "I am here to review this place...",
                new Color(130, 70, 160)));
    }

    private Appearances() {
    }

    static void register(String customerType, CustomerStyle style) {
        STYLES_BY_TYPE.put(customerType, style);
    }

    static CustomerStyle styleOf(Customer customer) {
        return STYLES_BY_TYPE.getOrDefault(customer.getTypeName(), DEFAULT);
    }

    static Look forCustomer(Customer customer) {
        return styleOf(customer).look.apply(customer.getNumber());
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

    /** VIP: velvet jacket with gold trim and sunglasses. */
    static Look vip(int n) {
        int i = n - 1;
        return new Look(SKINS[(i + 2) % SKINS.length], HAIRS[(i + 3) % HAIRS.length], Look.Hair.SHORT,
                new Color(110, 50, 140), new Color(40, 30, 50), Look.Accessory.VIP_SUIT, new Color(240, 190, 60));
    }

    /** Food critic: black turtleneck, red beret, round glasses and a notebook. */
    static Look critic(int n) {
        int i = n - 1;
        return new Look(SKINS[(i + 4) % SKINS.length], HAIRS[(i + 1) % HAIRS.length], Look.Hair.SHORT,
                new Color(44, 44, 52), new Color(70, 60, 56), Look.Accessory.CRITIC_BERET, new Color(190, 40, 50));
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
