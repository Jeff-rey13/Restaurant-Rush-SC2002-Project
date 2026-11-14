package rush.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Happy Hour pricing. The manager can turn it on once per game, at the start
 * of a turn. It lasts for that turn and the next.
 * While it is on, new orders get 20% off (after any customer discount), and at
 * the start of each Happy Hour turn every customer still waiting for a table or
 * food gets back 15 satisfaction (up to 100). Prices are fixed when an order
 * is taken, so older orders do not change.
 */
public class HappyHour implements PricingPolicy {

    public static final int DURATION_TURNS = 2;
    public static final int PERCENT_PAYABLE = 80;
    public static final int RECOVERY_POINTS = 15;

    private boolean used;
    private int remainingTurns; // Happy Hour turns left, counting this one

    public boolean isAvailable() {
        return !used;
    }

    public boolean isActive() {
        return remainingTurns > 0;
    }

    public int getRemainingTurns() {
        return remainingTurns;
    }

    public ActionResult activate() {
        if (used) {
            return ActionResult.fail("Happy Hour has already been used; it can be activated only once per game.");
        }
        used = true;
        remainingTurns = DURATION_TURNS;
        return ActionResult.ok("Happy Hour is ON for this turn and the next: orders taken now get 20% off, "
                + "waiting customers recover " + RECOVERY_POINTS + " satisfaction at the start of each active turn.");
    }

    @Override
    public int apply(int priceCents) {
        return isActive() ? Money.percentOf(priceCents, PERCENT_PAYABLE) : priceCents;
    }

    /** Start-of-turn recovery for customers still waiting for a table or food. */
    public List<String> applyRecovery(Restaurant restaurant) {
        List<String> events = new ArrayList<>();
        if (!isActive()) {
            return events;
        }
        for (Customer customer : restaurant.getCustomersWaitingForService()) {
            int before = customer.getSatisfaction();
            customer.recoverSatisfaction(RECOVERY_POINTS);
            events.add(before == customer.getSatisfaction()
                    ? "Happy Hour: " + customer.getLabel() + " is already at the maximum " + before + "."
                    : "Happy Hour: " + customer.getLabel() + " recovers " + before + " -> "
                    + customer.getSatisfaction() + ".");
        }
        return events;
    }

    /** Called at the end of each turn to count down the turns left. */
    public void endTurn() {
        if (remainingTurns > 0) {
            remainingTurns--;
        }
    }
}
