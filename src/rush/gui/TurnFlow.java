package rush.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import rush.control.GameController;
import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.Order;
import rush.domain.OrderStatus;
import rush.domain.Restaurant;

/**
 * Runs one turn in the window version: which staff member is choosing, which
 * choices to show, and when the turn ends. Like ConsoleUI, it has no game
 * rules. Every choice goes to GameController, which accepts it or returns the
 * reason it is not allowed.
 */
final class TurnFlow {

    enum Role { HOST, WAITER, CHEF }

    enum Kind { SEAT, TAKE_ORDER, SERVE, COOK, WAIT }

    enum Phase { HOST, WAITER, CHEF, TURN_END, GAME_OVER }

    enum LogKind { TURN, ARRIVAL, ACTION, REFUSED, EVENT }

    /** One choice the manager can make. */
    static final class Option {
        final Role role;
        final Kind kind;
        final String label;
        final String detail;
        final Customer customer;
        final Order order;
        final Supplier<ActionResult> action;

        Option(Role role, Kind kind, String label, String detail, Customer customer, Order order,
               Supplier<ActionResult> action) {
            this.role = role;
            this.kind = kind;
            this.label = label;
            this.detail = detail;
            this.customer = customer;
            this.order = order;
            this.action = action;
        }
    }

    /** Things the window reacts to (animation, sound, log). */
    interface Listener {
        void onLog(String text, LogKind kind);

        void onStaffAction(Option option, ActionResult result);

        void onHappyHour(ActionResult result);

        void onTurnEnded();

        void onGameOver();
    }

    private final Supplier<GameController> factory;
    private final Listener listener;
    private GameController game;
    private Phase phase = Phase.TURN_END;

    TurnFlow(Supplier<GameController> factory, Listener listener) {
        this.factory = factory;
        this.listener = listener;
    }

    GameController game() {
        return game;
    }

    Phase phase() {
        return phase;
    }

    void newGame() {
        game = factory.get();
        phase = Phase.TURN_END;
        nextTurn();
    }

    /** Starts the next turn: step 1 (arrivals), then the first staff choice. */
    void nextTurn() {
        if (game == null || !game.hasNextTurn()) {
            return;
        }
        List<String> events = game.beginTurn();
        listener.onLog("Turn " + game.getCurrentTurn() + " of " + game.getConfig().getTotalTurns(), LogKind.TURN);
        for (String e : events) {
            listener.onLog(e, LogKind.ARRIVAL);
        }
        advance();
    }

    /** Runs a choice. If it is refused, the phase stays the same so the manager can choose again. */
    void choose(Option option) {
        if (phase == Phase.TURN_END || phase == Phase.GAME_OVER) {
            return;
        }
        ActionResult result = option.action.get();
        listener.onLog(result.getMessage(), result.isSuccess() ? LogKind.ACTION : LogKind.REFUSED);
        listener.onStaffAction(option, result);
        if (result.isSuccess()) {
            advance();
        }
    }

    /** Happy Hour can be turned on once per game, at the start of a turn (the controller checks this). */
    boolean canActivateHappyHour() {
        return game != null && game.canActivateHappyHour() && phase == Phase.HOST;
    }

    void activateHappyHour() {
        if (phase == Phase.TURN_END || phase == Phase.GAME_OVER) {
            return;
        }
        ActionResult result = game.activateHappyHour();
        listener.onLog(result.getMessage(), result.isSuccess() ? LogKind.ACTION : LogKind.REFUSED);
        listener.onHappyHour(result);
    }

    /** Moves to the next staff member who has not acted yet (Host, Waiter, Chef). Ends the turn when all have. */
    private void advance() {
        if (game.isHostAvailable()) {
            phase = Phase.HOST;
        } else if (game.isWaiterAvailable()) {
            phase = Phase.WAITER;
        } else if (game.isChefAvailable()) {
            phase = Phase.CHEF;
        } else {
            for (String e : game.endTurn()) {
                listener.onLog(e, LogKind.EVENT);
            }
            if (game.hasNextTurn()) {
                phase = Phase.TURN_END;
                listener.onTurnEnded();
            } else {
                phase = Phase.GAME_OVER;
                listener.onGameOver();
            }
        }
    }

    // ----- options -----

    /** The choices for the current staff member, ending with Wait. */
    List<Option> options() {
        List<Option> list = new ArrayList<>();
        if (phase == Phase.HOST) {
            for (Customer c : game.getRestaurant().getWaitingQueue()) {
                list.add(hostSeat(c));
            }
            list.add(new Option(Role.HOST, Kind.WAIT, "Wait", "the Host does nothing this turn", null, null,
                    game::hostWait));
        } else if (phase == Phase.WAITER) {
            Restaurant r = game.getRestaurant();
            for (Customer c : r.getWaitingQueue()) {
                list.add(seat(c));
            }
            for (Customer c : r.getSeatedCustomers()) {
                if (c.getStatus() == CustomerStatus.SEATED) {
                    list.add(takeOrder(c));
                }
            }
            for (Order o : r.getActiveOrders()) {
                list.add(serve(o));
            }
            list.add(new Option(Role.WAITER, Kind.WAIT, "Wait", "do nothing this turn", null, null, game::waiterWait));
        } else if (phase == Phase.CHEF) {
            for (Order o : game.getRestaurant().getActiveOrders()) {
                list.add(cook(o));
            }
            list.add(new Option(Role.CHEF, Kind.WAIT, "Wait", "do nothing this turn", null, null, game::chefWait));
        }
        return list;
    }

    Option seat(Customer c) {
        return new Option(Role.WAITER, Kind.SEAT, "Seat " + name(c),
                "waiting in the queue, mood " + c.getSatisfaction(), c, null, () -> game.waiterSeat(c));
    }

    Option hostSeat(Customer c) {
        return new Option(Role.HOST, Kind.SEAT, "Host seats " + name(c),
                "waiting in the queue, mood " + c.getSatisfaction(), c, null, () -> game.hostSeat(c));
    }

    Option takeOrder(Customer c) {
        return new Option(Role.WAITER, Kind.TAKE_ORDER, "Take " + name(c) + "'s order",
                "seated at Table " + c.getTable().getNumber(), c, null, () -> game.waiterTakeOrder(c));
    }

    Option serve(Order o) {
        String state = o.getStatus() == OrderStatus.READY ? "READY on the pass" : "still cooking, " + o.getProgress();
        return new Option(Role.WAITER, Kind.SERVE, "Serve " + o.getItem().getName() + " to " + name(o.getCustomer()),
                state, o.getCustomer(), o, () -> game.waiterServe(o));
    }

    Option cook(Order o) {
        String state = o.getStatus() == OrderStatus.READY ? "already READY" : o.getProgress() + " done";
        return new Option(Role.CHEF, Kind.COOK, "Cook " + o.getItem().getName() + " (" + o.getLabel().replace("Order ", "") + ")",
                state + ", for " + name(o.getCustomer()), o.getCustomer(), o, () -> game.chefPrepare(o));
    }

    Option waitOption(Role role) {
        if (role == Role.HOST) {
            return new Option(role, Kind.WAIT, "Wait", "", null, null, game::hostWait);
        }
        return role == Role.WAITER
                ? new Option(role, Kind.WAIT, "Wait", "", null, null, game::waiterWait)
                : new Option(role, Kind.WAIT, "Wait", "", null, null, game::chefWait);
    }

    static String name(Customer c) {
        return c.getLabel() + " " + Appearances.nameOf(c);
    }
}
