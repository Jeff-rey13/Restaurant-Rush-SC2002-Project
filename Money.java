package rush.domain;

/**
 * Money helpers. All amounts are stored as whole cents so prices like $24.30
 * or $10.80 never get floating-point errors.
 */
public final class Money {

    private Money() {
        // helper class, no instances
    }

    /** Formats cents as dollars with two decimals, e.g. 2430 -> "$24.30". */
    public static String format(int cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }

    /**
     * Returns {@code percent}% of {@code cents}, rounded half up to the nearest cent.
     * Example: percentOf(2700, 90) = 2430.
     */
    public static int percentOf(int cents, int percent) {
        long scaled = (long) cents * percent;
        return (int) ((scaled + 50) / 100);
    }
}
