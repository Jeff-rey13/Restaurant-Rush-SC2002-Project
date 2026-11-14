package rush.control;

import java.util.ArrayList;
import java.util.List;

import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.Menu;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.PriceCalculator;
import rush.domain.Restaurant;
import rush.staff.Cashier;
import rush.staff.Chef;
import rush.staff.Staff;
import rush.staff.Waiter;

/**
 * Runs the turn sequence (Section 3.2). The UI calls beginTurn(), then passes
 * the manager's staff choices, then calls endTurn(). All game rules are
 * applied here or in the domain/staff objects, never in the UI.
 */
public class GameController {

    private final GameConfig config;
    private final ArrivalSchedule schedule;
    private final Restaurant restaurant;
    private final PriceCalculator pricing;
    private final GameStats stats = new GameStats();

    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();

    private int currentTurn;

    public GameController(GameConfig config, ArrivalSchedule schedule, Menu menu) {
        this.config = config;
        this.schedule = schedule;
        this.restaurant = new Restaurant(menu, config.getTableCount());
        this.pricing = new PriceCalculator();
    }

    /** Creates a game with the required base setting. */
    public static GameController createBaseGame() {
        return new GameController(GameConfig.base(), ArrivalSchedule.base(), Menu.createDefault());
    }

    // ---------------------------------------------------------------- turn flow

    public boolean hasNextTurn() {
        return currentTurn < config.getTotalTurns();
    }

    /** Step 1: new turn, staff reset, scheduled arrival joins the queue. */
    public List<String> beginTurn() {
        if (!hasNextTurn()) {
            throw new IllegalStateException("The game is over");
        }
        currentTurn++;
        for (Staff staff : getStaff()) {
            staff.startTurn();
        }
        List<String> events = new ArrayList<>();
        Customer arrival = schedule.arrivalFor(currentTurn);
        if (arrival != null) {
            restaurant.admit(arrival);
            events.add(arrival.getLabel() + " (" + arrival.getTypeName()
                    + ") arrives and joins the waiting queue with satisfaction "
                    + arrival.getSatisfaction() + ".");
        }
        return events;
    }

    // ------------------------------------------------- step 2/3: manager choices

    public ActionResult waiterSeat(Customer customer) {
        return waiter.seat(customer, restaurant);
    }

    public ActionResult waiterTakeOrder(Customer customer) {
        return waiter.takeOrder(customer, restaurant, pricing, currentTurn);
    }

    public ActionResult waiterServe(Order order) {
        ActionResult result = waiter.serve(order, currentTurn);
        if (result.isSuccess()) {
            stats.recordServed();
        }
        return result;
    }

    public ActionResult waiterWait() {
        return waiter.waitThisTurn();
    }

    public ActionResult chefPrepare(Order order) {
        return chef.prepare(order, currentTurn);
    }

    public ActionResult chefWait() {
        return chef.waitThisTurn();
    }

    /** Steps 4 and 5: eating, payment, waiting penalty and abandonment. */
    public List<String> endTurn() {
        List<String> events = new ArrayList<>();

        // Step 4a: customers served on an earlier turn finish eating.
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.finishEatingIfDone(currentTurn)) {
                events.add(customer.getLabel() + " finishes eating and is READY_TO_PAY (pays next turn at the earliest).");
            }
        }

        // Step 4b: the Cashier charges at most one customer, oldest first.
        Customer payer = cashier.collectPayment(restaurant, currentTurn);
        if (payer != null) {
            int amount = payer.getOrder().getPriceCents();
            stats.recordPayment(amount, payer.getPaidSatisfaction());
            events.add("Cashier collects " + Money.format(amount) + " from " + payer.getLabel()
                    + ": PAID, satisfaction " + payer.getPaidSatisfaction() + " recorded, table freed.");
        }

        // Step 5: everyone not yet served loses satisfaction; at 0 they leave.
        List<String> penalties = new ArrayList<>();
        for (Customer customer : restaurant.getCustomersWaitingForService()) {
            boolean leaves = customer.loseSatisfactionForWaiting();
            penalties.add(customer.getLabel() + " -" + customer.getSatisfactionLossPerTurn()
                    + " (now " + customer.getSatisfaction() + ")");
            if (leaves) {
                restaurant.removeUnhappyCustomer(customer);
                stats.recordUnhappyDeparture();
                events.add(customer.getLabel() + " reached 0 satisfaction and LEAVES without paying"
                        + (customer.getOrder() != null ? "; " + customer.getOrder().getLabel() + " cancelled." : "."));
            }
        }
        if (!penalties.isEmpty()) {
            events.add("Waiting penalty: " + String.join(", ", penalties) + ".");
        }
        return events;
    }

    // ---------------------------------------------------------------- queries

    public int getCurrentTurn() {
        return currentTurn;
    }

    public GameConfig getConfig() {
        return config;
    }

    public GameStats getStats() {
        return stats;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public List<Staff> getStaff() {
        List<Staff> staff = new ArrayList<>();
        staff.add(waiter);
        staff.add(chef);
        staff.add(cashier);
        return staff;
    }

    public boolean isWaiterAvailable() {
        return waiter.isAvailable();
    }

    public boolean isChefAvailable() {
        return chef.isAvailable();
    }

    /** Summary of what each role could usefully do right now (start-of-turn display). */
    public List<String> describeAvailableTasks() {
        List<String> lines = new ArrayList<>();
        int seatable = restaurant.hasFreeTable() ? restaurant.getWaitingQueue().size() : 0;
        int toOrder = 0;
        for (Customer customer : restaurant.getSeatedCustomers()) {
            if (customer.getOrder() == null) {
                toOrder++;
            }
        }
        int toServe = 0;
        int toCook = 0;
        for (Order order : restaurant.getActiveOrders()) {
            if (order.isReadyToServe(currentTurn)) {
                toServe++;
            } else if (order.getRemainingUnits() > 0) {
                toCook++;
            }
        }
        lines.add("Waiter : " + waiter.describeDuties() + " -> " + seatable + " can be seated, "
                + toOrder + " order(s) to take, " + toServe + " dish(es) to serve");
        lines.add("Chef   : " + chef.describeDuties() + " -> " + toCook + " order(s) to cook");
        lines.add("Cashier: " + cashier.describeDuties());
        return lines;
    }

    public GameResult getResult() {
        return stats.evaluate(config);
    }
}
