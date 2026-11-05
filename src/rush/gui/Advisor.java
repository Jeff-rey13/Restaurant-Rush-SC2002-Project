package rush.gui;

import java.util.List;

import rush.domain.Customer;
import rush.domain.Order;
import rush.domain.OrderStatus;
import rush.domain.Restaurant;

/**
 * The "Manager's tip" shown above the choices. It looks at the restaurant and
 * suggests a good next step, so a new player always knows what to do. It only
 * gives advice: the player can pick anything, and the controller still decides
 * what is allowed.
 */
final class Advisor {

    /** A tip: the text to show and which option to highlight (can be null). */
    static final class Tip {
        final String text;
        final TurnFlow.Option recommended;

        Tip(String text, TurnFlow.Option recommended) {
            this.text = text;
            this.recommended = recommended;
        }
    }

    private Advisor() {
    }

    static Tip advise(TurnFlow flow, List<TurnFlow.Option> options) {
        if (flow.game() == null) {
            return new Tip("", null);
        }
        Restaurant r = flow.game().getRestaurant();
        switch (flow.phase()) {
            case HOST:
                return hostTip(r, options, flow);
            case WAITER:
                return waiterTip(r, options);
            case CHEF:
                return chefTip(options);
            case TURN_END:
                return new Tip("Everyone has acted. Press Next turn (Enter) to see who arrives next.", null);
            default:
                return new Tip("", null);
        }
    }

    private static Tip waiterTip(Restaurant r, List<TurnFlow.Option> options) {
        TurnFlow.Option best = null;
        for (TurnFlow.Option o : options) {
            if (o.kind == TurnFlow.Kind.SERVE && o.order.getStatus() == OrderStatus.READY
                    && (best == null || urgency(o.customer) < urgency(best.customer))) {
                best = o;
            }
        }
        if (best != null) {
            return new Tip(first(best.customer) + "'s " + best.order.getItem().getName()
                    + " is ready on the pass. Serve it now: once served, a customer stops losing satisfaction.", best);
        }
        for (TurnFlow.Option o : options) {
            if (o.kind == TurnFlow.Kind.TAKE_ORDER) {
                return new Tip(first(o.customer) + " (" + o.customer.getTypeName() + ") is seated but has not ordered. "
                        + "Take the order so the Chef can start cooking.", o);
            }
        }
        if (!r.getWaitingQueue().isEmpty()) {
            Customer next = r.getWaitingQueue().get(0);
            if (r.hasFreeTable()) {
                for (TurnFlow.Option o : options) {
                    if (o.kind == TurnFlow.Kind.SEAT && o.customer == lowestInQueue(r)) {
                        return new Tip(first(o.customer) + " is waiting at the door and a table is free. Seat "
                                + first(o.customer) + " now: people in the queue lose satisfaction every turn.", o);
                    }
                }
            }
            return new Tip("Both tables are taken, so " + first(next) + " must wait until someone pays. "
                    + "Nothing else needs the Waiter: choose Wait.", waitOption(options));
        }
        for (Order o : r.getActiveOrders()) {
            if (o.getStatus() == OrderStatus.IN_PREPARATION) {
                return new Tip(first(o.getCustomer()) + "'s " + o.getItem().getName() + " is still cooking ("
                        + o.getProgress() + "). The Waiter can only Wait this turn.", waitOption(options));
            }
        }
        return new Tip("Nothing needs the Waiter right now. Choose Wait.", waitOption(options));
    }

    private static Tip hostTip(Restaurant r, List<TurnFlow.Option> options, TurnFlow flow) {
        String prefix = suggestHappyHour(flow)
                ? "Happy Hour could help now (press Y): waiting guests recover 15 for two turns, but orders are 20% off. " : "";
        if (!r.getWaitingQueue().isEmpty() && r.hasFreeTable()) {
            Customer next = lowestInQueue(r);
            for (TurnFlow.Option o : options) {
                if (o.kind == TurnFlow.Kind.SEAT && o.customer == next) {
                    return new Tip(prefix + "Let the Host seat " + first(next)
                            + " now - that keeps the Waiter free for another task this turn.", o);
                }
            }
        }
        String why = r.getWaitingQueue().isEmpty() ? "Nobody is waiting at the door" : "There is no free table";
        return new Tip(prefix + why + ", so the Host can Wait.", waitOption(options));
    }

    /** Suggests Happy Hour if it is still available and several guests are waiting or someone is about to leave. */
    static boolean suggestHappyHour(TurnFlow flow) {
        if (!flow.canActivateHappyHour()) {
            return false;
        }
        List<Customer> waiting = flow.game().getRestaurant().getCustomersWaitingForService();
        int count = 0;
        for (Customer c : waiting) {
            count++;
            if (urgency(c) < 4) {
                return true;
            }
        }
        return count >= 2;
    }

    private static Tip chefTip(List<TurnFlow.Option> options) {
        TurnFlow.Option best = null;
        for (TurnFlow.Option o : options) {
            if (o.kind == TurnFlow.Kind.COOK && o.order.getStatus() == OrderStatus.IN_PREPARATION
                    && (best == null || urgency(o.customer) < urgency(best.customer))) {
                best = o;
            }
        }
        if (best != null) {
            Order o = best.order;
            int left = o.getRemainingUnits();
            return new Tip("Cook " + first(o.getCustomer()) + "'s " + o.getItem().getName() + " (" + o.getProgress()
                    + "). " + (left == 1 ? "One more unit and it is READY for the Waiter next turn."
                    : left + " units to go, so start now."), best);
        }
        return new Tip("There is nothing to cook yet. Choose Wait.", waitOption(options));
    }

    private static Customer lowestInQueue(Restaurant r) {
        Customer worst = null;
        for (Customer c : r.getWaitingQueue()) {
            if (worst == null || urgency(c) < urgency(worst)) {
                worst = c;
            }
        }
        return worst;
    }

    /**
     * How many more turns this customer can wait before leaving. Customer types
     * lose patience at different speeds, so this compares them fairly (lower = more urgent).
     */
    static double urgency(Customer c) {
        return c.getSatisfaction() / (double) Math.max(1, c.getSatisfactionLossPerTurn());
    }

    private static TurnFlow.Option waitOption(List<TurnFlow.Option> options) {
        for (TurnFlow.Option o : options) {
            if (o.kind == TurnFlow.Kind.WAIT) {
                return o;
            }
        }
        return null;
    }

    static String first(Customer c) {
        return Appearances.nameOf(c);
    }

}
