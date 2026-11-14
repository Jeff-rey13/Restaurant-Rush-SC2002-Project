package rush.control;

import java.util.ArrayList;
import java.util.List;

import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.HappyHour;
import rush.domain.Menu;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.PriceCalculator;
import rush.domain.Restaurant;
import rush.staff.Cashier;
import rush.staff.Chef;
import rush.staff.Host;
import rush.staff.Staff;
import rush.staff.Waiter;

/**
 * Runs each turn (Section 3.2). The UI calls beginTurn(), then sends the
 * manager's choices (Happy Hour if wanted, then Host, Waiter and Chef, in that
 * order), then calls endTurn(). All game rules are checked here or in the
 * domain and staff classes, never in the UI.
 */
public class GameController {

    private final GameConfig config;
    private final ArrivalSchedule schedule;
    private final Restaurant restaurant;
    private final PriceCalculator pricing;
    private final GameStats stats = new GameStats();
    private final HappyHour happyHour = new HappyHour();

    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();
    private final Host host = new Host();

    private int currentTurn;

    public GameController(GameConfig config, ArrivalSchedule schedule, Menu menu) {
        this.config = config;
        this.schedule = schedule;
        this.restaurant = new Restaurant(menu, config.getTableCount());
        this.pricing = new PriceCalculator();
        this.pricing.addPolicy(happyHour);
    }

    /** Creates a game with the base settings. */
    public static GameController createBaseGame() {
        return new GameController(GameConfig.base(), ArrivalSchedule.base(), Menu.createDefault());
    }

    // ----- turn flow -----

    public boolean hasNextTurn() {
        return currentTurn < config.getTotalTurns();
    }

    /** Step 1: start a new turn, reset the staff and add any new arrival to the queue. */
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
                    + arrival.getSatisfaction() + ". Behaviour: " + arrival.describeBehaviour() + ".");
        }
        // Happy Hour is still on from last turn, so waiting customers recover now.
        events.addAll(happyHour.applyRecovery(restaurant));
        return events;
    }

    // ----- Happy Hour -----

    public boolean canActivateHappyHour() {
        return happyHour.isAvailable();
    }

    /** Can only be turned on at the start of a turn, before any staff member acts. */
    public ActionResult activateHappyHour() {
        if (!host.isAvailable() || !waiter.isAvailable() || !chef.isAvailable()) {
            return ActionResult.fail("Happy Hour can only be switched on at the start of a turn, before staff act.");
        }
        ActionResult result = happyHour.activate();
        if (!result.isSuccess()) {
            return result;
        }
        // This is the first Happy Hour turn, so the recovery happens now.
        List<String> recovery = happyHour.applyRecovery(restaurant);
        return recovery.isEmpty() ? result
                : ActionResult.ok(result.getMessage() + " " + String.join(" ", recovery));
    }

    public HappyHour getHappyHour() {
        return happyHour;
    }

    // ----- Host -----

    public ActionResult hostSeat(Customer customer) {
        return host.seat(customer, restaurant);
    }

    public ActionResult hostWait() {
        return host.waitThisTurn();
    }

    public boolean isHostAvailable() {
        return host.isAvailable();
    }

    // ----- step 2/3: manager choices -----

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

    /** Steps 4 and 5: eating, paying, losing satisfaction and leaving. */
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

        // Step 5: everyone not served yet loses satisfaction. At 0 they leave.
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

        boolean wasActive = happyHour.isActive();
        happyHour.endTurn();
        if (wasActive && !happyHour.isActive()) {
            events.add("Happy Hour has ended.");
        }
        return events;
    }

    // ----- queries -----

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
        staff.add(host);
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

    /** What each role can do right now (shown at the start of the turn). */
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
        lines.add("Host   : " + host.describeDuties() + " -> " + seatable + " can be seated");
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
