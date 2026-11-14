package rush.gui;

import java.util.List;

import rush.domain.Customer;
import rush.domain.Order;
import rush.domain.OrderStatus;
import rush.domain.Restaurant;

/**
 * The "Manager's tip" shown above the choices: it reads the restaurant and
 * suggests a sensible next step, so a new player always knows what to do.
 * It only gives advice; the player can choose anything, and the controller
 * still decides what is allowed.
 */
final class Advisor {

    /** A suggestion: the words to show and which option to highlight (may be null). */
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
                    && (best == null || o.customer.getSatisfaction() < best.customer.getSatisfaction())) {
                best = o;
            }
        }
        if (best != null) {
            return new Tip(first(best.customer) + "'s " + best.order.getItem().getName()
                    + " is ready on the pass. Serve it now: once served, a customer stops losing satisfaction.", best);
        }
        for (TurnFlow.Option o : options) {
            if (o.kind == TurnFlow.Kind.TAKE_ORDER) {
                return new Tip(first(o.customer) + " is seated but has not ordered. Take the order so the Chef "
                        + "can start cooking.", o);
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

    private static Tip chefTip(List<TurnFlow.Option> options) {
        TurnFlow.Option best = null;
        for (TurnFlow.Option o : options) {
            if (o.kind == TurnFlow.Kind.COOK && o.order.getStatus() == OrderStatus.IN_PREPARATION
                    && (best == null || o.customer.getSatisfaction() < best.customer.getSatisfaction())) {
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
            if (worst == null || c.getSatisfaction() < worst.getSatisfaction()) {
                worst = c;
            }
        }
        return worst;
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
