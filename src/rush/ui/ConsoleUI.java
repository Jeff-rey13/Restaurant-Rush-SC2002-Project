package rush.ui;

import java.io.PrintStream;
import java.util.List;

import rush.control.GameConfig;
import rush.control.GameController;
import rush.control.GameResult;
import rush.control.GameStats;
import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.HappyHour;
import rush.domain.MenuItem;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.Table;

/**
 * Boundary class. It only shows the game state and collects the manager's
 * choices. All rule checks happen in the controller and domain classes.
 */
public class ConsoleUI {

    private static final String LINE = "------------------------------------------------------------------";

    private final GameController game;
    private final InputReader input;
    private final PrintStream out;

    public ConsoleUI(GameController game, InputReader input, PrintStream out) {
        this.game = game;
        this.input = input;
        this.out = out;
    }

    public void run() {
        showWelcome();
        try {
            while (game.hasNextTurn()) {
                playTurn();
            }
            showFinalResult();
        } catch (InputReader.InputEndedException e) {
            out.println();
            out.println("Input ended before the game finished (turn " + game.getCurrentTurn() + "). Game stopped.");
        }
    }

    // ----- one turn -----

    private void playTurn() {
        List<String> arrivals = game.beginTurn();
        out.println();
        out.println("==================== TURN " + game.getCurrentTurn() + " / "
                + game.getConfig().getTotalTurns() + " ====================");
        printEvents(arrivals);
        showStatus();
        happyHourPhase();
        hostPhase();
        waiterPhase();
        chefPhase();
        out.println(LINE);
        printEvents(game.endTurn());
        showEndOfTurn();
    }

    private void happyHourPhase() {
        if (!game.canActivateHappyHour()) {
            return;
        }
        out.println();
        out.println("HAPPY HOUR (once per game): 20% off orders taken this turn and next;");
        out.println("customers waiting for a table or food recover " + HappyHour.RECOVERY_POINTS
                + " satisfaction at the start of each active turn.");
        out.println("  1) Switch Happy Hour on now");
        out.println("  0) Not now");
        if (input.readChoice("Happy Hour", 0, 1) == 1) {
            report(game.activateHappyHour());
        }
    }

    private void hostPhase() {
        while (game.isHostAvailable()) {
            out.println();
            out.println("HOST - seat one queued customer (in addition to the Waiter), or wait:");
            List<Customer> queue = game.getRestaurant().getWaitingQueue();
            for (int i = 0; i < queue.size(); i++) {
                out.println("  " + (i + 1) + ") Seat " + describeCustomer(queue.get(i)));
            }
            out.println("  0) Wait");
            int choice = input.readChoice("Host task", 0, queue.size());
            report(choice == 0 ? game.hostWait() : game.hostSeat(queue.get(choice - 1)));
        }
    }

    private void waiterPhase() {
        while (game.isWaiterAvailable()) {
            out.println();
            out.println("WAITER - choose one task:");
            out.println("  1) Seat a queued customer");
            out.println("  2) Take a seated customer's order");
            out.println("  3) Serve a READY dish");
            out.println("  0) Wait");
            int choice = input.readChoice("Waiter task", 0, 3);
            ActionResult result;
            if (choice == 0) {
                result = game.waiterWait();
            } else if (choice == 1) {
                Customer customer = pickCustomer("Seat which customer?", game.getRestaurant().getWaitingQueue(),
                        "Nobody is in the waiting queue.");
                result = customer == null ? null : game.waiterSeat(customer);
            } else if (choice == 2) {
                Customer customer = pickCustomer("Take whose order?", game.getRestaurant().getSeatedCustomers(),
                        "No customer is seated at a table.");
                result = customer == null ? null : game.waiterTakeOrder(customer);
            } else {
                Order order = pickOrder("Serve which order?", "There are no orders in the kitchen.");
                result = order == null ? null : game.waiterServe(order);
            }
            report(result);
        }
    }

    private void chefPhase() {
        while (game.isChefAvailable()) {
            out.println();
            out.println("CHEF - choose an order to advance by one preparation unit, or wait:");
            List<Order> orders = game.getRestaurant().getActiveOrders();
            for (int i = 0; i < orders.size(); i++) {
                out.println("  " + (i + 1) + ") " + describeOrder(orders.get(i)));
            }
            out.println("  0) Wait");
            int choice = input.readChoice("Chef task", 0, orders.size());
            ActionResult result = choice == 0 ? game.chefWait() : game.chefPrepare(orders.get(choice - 1));
            report(result);
        }
    }

    /** Shows the options and returns the chosen one, or null for "back". */
    private Customer pickCustomer(String question, List<Customer> candidates, String emptyMessage) {
        if (candidates.isEmpty()) {
            out.println("  Cannot do that: " + emptyMessage + " Choose again.");
            return null;
        }
        out.println("  " + question);
        for (int i = 0; i < candidates.size(); i++) {
            out.println("    " + (i + 1) + ") " + describeCustomer(candidates.get(i)));
        }
        out.println("    0) Back");
        int choice = input.readChoice("  Customer", 0, candidates.size());
        return choice == 0 ? null : candidates.get(choice - 1);
    }

    private Order pickOrder(String question, String emptyMessage) {
        List<Order> orders = game.getRestaurant().getActiveOrders();
        if (orders.isEmpty()) {
            out.println("  Cannot do that: " + emptyMessage + " Choose again.");
            return null;
        }
        out.println("  " + question);
        for (int i = 0; i < orders.size(); i++) {
            out.println("    " + (i + 1) + ") " + describeOrder(orders.get(i)));
        }
        out.println("    0) Back");
        int choice = input.readChoice("  Order", 0, orders.size());
        return choice == 0 ? null : orders.get(choice - 1);
    }

    private void report(ActionResult result) {
        if (result == null) {
            return; // the manager went back, so nothing was tried
        }
        if (result.isSuccess()) {
            out.println("  OK: " + result.getMessage());
        } else {
            out.println("  NOT ALLOWED: " + result.getMessage() + " The task was not used up - choose again.");
        }
    }

    // ----- display -----

    private void showWelcome() {
        GameConfig config = game.getConfig();
        out.println(LINE);
        out.println(" RESTAURANT RUSH - you are the manager");
        out.println(LINE);
        out.println(" " + config.getTotalTurns() + " turns, " + config.getTableCount()
                + " tables, one Host, one Waiter, one Chef, one Cashier.");
        out.println(" Each turn: optional Happy Hour (once per game), then Host, Waiter, Chef act in that order.");
        out.println(" Goal after the last turn: at least " + config.getTargetPaidCustomers()
                + " paid customers, revenue at least " + Money.format(config.getTargetRevenueCents())
                + ", average satisfaction of paid customers at least " + config.getTargetAverageSatisfaction() + ".");
        out.println(" Menu:");
        for (MenuItem item : game.getRestaurant().getMenu().getItems()) {
            out.println("   - " + item);
        }
        out.println(" Customers choose their own food: customer number n orders menu item");
        out.println(" number ((n - 1) mod " + game.getRestaurant().getMenu().getItems().size()
                + ") + 1 in the list above.");
    }

    private void showStatus() {
        GameStats stats = game.getStats();
        out.println("Revenue: " + Money.format(stats.getRevenueCents())
                + " | Paid customers: " + stats.getPaidCustomers()
                + " | Avg satisfaction (paid): " + formatAverage(stats));
        out.println("Happy Hour: " + describeHappyHour(game.getHappyHour()));
        List<Customer> queue = game.getRestaurant().getWaitingQueue();
        out.println(queue.isEmpty() ? "Waiting queue: (empty)" : "Waiting queue:");
        for (Customer customer : queue) {
            out.println("  " + describeCustomer(customer));
        }
        out.println("Tables:");
        for (Table table : game.getRestaurant().getTables()) {
            out.println("  " + table.getLabel() + ": "
                    + (table.isFree() ? "(free)" : describeCustomer(table.getOccupant())));
        }
        out.println("Staff tasks available:");
        for (String line : game.describeAvailableTasks()) {
            out.println("  " + line);
        }
    }

    private void showEndOfTurn() {
        GameStats stats = game.getStats();
        out.println("END OF TURN " + game.getCurrentTurn() + ": served " + stats.getServedCount()
                + " | unhappy departures " + stats.getUnhappyDepartures()
                + " | revenue " + Money.format(stats.getRevenueCents())
                + " | avg satisfaction (paid) " + formatAverage(stats)
                + " | turns used " + game.getCurrentTurn() + "/" + game.getConfig().getTotalTurns());
    }

    private void showFinalResult() {
        GameStats stats = game.getStats();
        GameConfig config = game.getConfig();
        out.println();
        out.println("==================== FINAL RESULT ====================");
        out.println(check(stats.meetsPaidTarget(config)) + " Paid customers: " + stats.getPaidCustomers()
                + " (target >= " + config.getTargetPaidCustomers() + ")");
        out.println(check(stats.meetsRevenueTarget(config)) + " Revenue: " + Money.format(stats.getRevenueCents())
                + " (target >= " + Money.format(config.getTargetRevenueCents()) + ")");
        out.println(check(stats.meetsSatisfactionTarget(config)) + " Avg satisfaction (paid): "
                + formatAverage(stats) + " (target >= " + config.getTargetAverageSatisfaction() + ")");
        out.println("Served: " + stats.getServedCount() + " | Unhappy departures: " + stats.getUnhappyDepartures()
                + " | Turns used: " + game.getCurrentTurn());
        out.println();
        out.println(game.getResult() == GameResult.VICTORY
                ? "*** VICTORY! The restaurant hit every target. ***"
                : "*** DEFEAT. At least one target was missed. ***");
    }

    private void printEvents(List<String> events) {
        for (String event : events) {
            out.println("* " + event);
        }
    }

    private String describeCustomer(Customer customer) {
        StringBuilder text = new StringBuilder();
        text.append(customer.getLabel()).append(" [").append(customer.getTypeName())
                .append(", satisfaction ").append(customer.getSatisfaction()).append("] ")
                .append(customer.getStatus());
        Order order = customer.getOrder();
        if (order == null) {
            if (customer.getTable() != null) {
                text.append(" - no order yet");
            }
        } else {
            text.append(" - ").append(order.getLabel()).append(' ').append(order.getItem().getName())
                    .append(' ').append(order.getStatus());
            if (order.isActive()) {
                text.append(" (").append(order.getProgress()).append(')');
            }
        }
        return text.toString();
    }

    private String describeOrder(Order order) {
        return order.getLabel() + " for " + order.getCustomer().getLabel() + ": "
                + order.getItem().getName() + ", " + order.getStatus() + " (" + order.getProgress() + ")";
    }

    private static String describeHappyHour(HappyHour happyHour) {
        if (happyHour.isActive()) {
            return happyHour.getRemainingTurns() == 1
                    ? "ACTIVE - last turn (20% off orders taken now)"
                    : "ACTIVE - " + happyHour.getRemainingTurns() + " turns left including this one (20% off orders taken now)";
        }
        return happyHour.isAvailable() ? "available (not used yet)" : "used";
    }

    private static String formatAverage(GameStats stats) {
        return stats.hasPaidCustomers() ? String.format("%.1f", stats.getAverageSatisfaction()) : "N/A";
    }

    private static String check(boolean ok) {
        return ok ? "[MET]   " : "[MISSED]";
    }
}
