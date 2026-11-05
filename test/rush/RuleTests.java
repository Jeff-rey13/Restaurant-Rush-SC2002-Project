package rush;

import java.util.ArrayList;
import java.util.List;

import rush.control.ArrivalSchedule;
import rush.control.GameConfig;
import rush.control.GameController;
import rush.control.GameResult;
import rush.domain.ActionResult;
import rush.domain.ComboMeal;
import rush.domain.CriticCustomer;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.Menu;
import rush.domain.MenuItem;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.OrderStatus;
import rush.domain.PriceCalculator;
import rush.domain.RegularCustomer;
import rush.domain.VIPCustomer;

/**
 * Simple rule tests in plain Java (no libraries, so they run offline with just javac).
 * Run with ./run-tests.sh. Each test uses the controller directly without the UI,
 * which also shows that the rules are not in the UI.
 */
public final class RuleTests {

    private static int passed;
    private static final List<String> failures = new ArrayList<>();

    private RuleTests() {
    }

    public static void main(String[] args) {
        moneyIsRoundedToNearestCent();
        exampleFromBriefSection33();
        invalidChoiceDoesNotUseUpTask();
        dishFinishedThisTurnCannotBeServedThisTurn();
        neglectedCustomerLeavesAndOrderIsCancelled();
        idleGameIsDefeat();
        customerTypesBehaveDifferently();
        criticReactsToServiceSpeed();
        comboMealIsSumOfComponents();
        happyHourPricingAndLimits();
        hostAndWaiterCanBothSeat();

        System.out.println(passed + " checks passed, " + failures.size() + " failed.");
        for (String failure : failures) {
            System.out.println("  FAILED: " + failure);
        }
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ----- tests -----

    private static void moneyIsRoundedToNearestCent() {
        check("Salad+Pasta combo price", Money.percentOf(900 + 1800, 90) == 2430);
        check("VIP burger in Happy Hour", Money.percentOf(Money.percentOf(1500, 90), 80) == 1080);
        check("format", Money.format(2430).equals("$24.30"));
        check("format small", Money.format(5).equals("$0.05"));
    }

    /** Brief 3.3: one Regular customer orders Pasta and gets full attention. */
    private static void exampleFromBriefSection33() {
        ArrivalSchedule schedule = new ArrivalSchedule();
        schedule.add(1, RegularCustomer::new);
        Menu pastaOnly = new Menu();
        pastaOnly.add(new MenuItem("Pasta", 1800, 2));
        GameController game = new GameController(GameConfig.base(), schedule, pastaOnly);

        game.beginTurn();                  // turn 1: arrives, seated
        Customer c1 = game.getRestaurant().getWaitingQueue().get(0);
        game.waiterSeat(c1);
        game.chefWait();
        game.endTurn();
        check("3.3 turn 1 -> 92", c1.getSatisfaction() == 92);

        game.beginTurn();                  // turn 2: order taken, unit 1 of 2
        game.waiterTakeOrder(c1);
        Order pasta = c1.getOrder();
        game.chefPrepare(pasta);
        game.endTurn();
        check("3.3 turn 2 -> 84", c1.getSatisfaction() == 84);

        game.beginTurn();                  // turn 3: unit 2 of 2, READY but cannot be served yet
        ActionResult early = game.waiterServe(pasta);
        check("cannot serve unfinished dish", !early.isSuccess());
        check("failed serve keeps the waiter free", game.isWaiterAvailable());
        game.waiterWait();
        game.chefPrepare(pasta);
        check("pasta READY after 2 units", pasta.getStatus() == OrderStatus.READY);
        game.endTurn();
        check("3.3 turn 3 -> 76", c1.getSatisfaction() == 76);

        game.beginTurn();                  // turn 4: served
        check("serve READY pasta", game.waiterServe(pasta).isSuccess());
        game.chefWait();
        game.endTurn();
        check("3.3 turn 4 no further loss", c1.getSatisfaction() == 76);

        game.beginTurn();                  // turn 5: finishes eating
        game.waiterWait();
        game.chefWait();
        game.endTurn();
        check("3.3 turn 5 READY_TO_PAY", c1.getStatus() == CustomerStatus.READY_TO_PAY);

        game.beginTurn();                  // turn 6: pays
        game.waiterWait();
        game.chefWait();
        game.endTurn();
        check("3.3 turn 6 PAID", c1.getStatus() == CustomerStatus.PAID);
        check("3.3 recorded satisfaction 76", c1.getPaidSatisfaction() == 76);
        check("3.3 revenue $18.00", game.getStats().getRevenueCents() == 1800);
        check("3.3 table freed", game.getRestaurant().getTables().get(0).isFree());
    }

    private static void invalidChoiceDoesNotUseUpTask() {
        GameController game = GameController.createBaseGame();
        game.beginTurn();
        Customer c1 = game.getRestaurant().getWaitingQueue().get(0);
        check("take order of unseated customer fails", !game.waiterTakeOrder(c1).isSuccess());
        check("waiter still available after failure", game.isWaiterAvailable());
        check("seat succeeds", game.waiterSeat(c1).isSuccess());
        check("waiter used up after success", !game.isWaiterAvailable());
        check("second task in same turn fails", !game.waiterWait().isSuccess());
    }

    private static void dishFinishedThisTurnCannotBeServedThisTurn() {
        GameController game = GameController.createBaseGame();
        game.beginTurn();
        Customer c1 = game.getRestaurant().getWaitingQueue().get(0);
        game.waiterSeat(c1);
        game.chefWait();
        game.endTurn();
        game.beginTurn();
        game.waiterTakeOrder(c1);
        game.chefPrepare(c1.getOrder());
        check("order READY this turn", c1.getOrder().getStatus() == OrderStatus.READY);
        check("not servable in the turn it became READY", !c1.getOrder().isReadyToServe(game.getCurrentTurn()));
    }

    private static void neglectedCustomerLeavesAndOrderIsCancelled() {
        ArrivalSchedule schedule = new ArrivalSchedule();
        schedule.add(1, RegularCustomer::new);
        GameController game = new GameController(GameConfig.base(), schedule, Menu.createDefault());
        game.beginTurn();
        Customer c1 = game.getRestaurant().getWaitingQueue().get(0);
        game.waiterSeat(c1);
        game.chefWait();
        game.endTurn();
        game.beginTurn();
        game.waiterTakeOrder(c1);
        game.chefWait();
        game.endTurn();
        while (c1.getStatus() != CustomerStatus.LEFT && game.hasNextTurn()) {
            game.beginTurn();
            game.waiterWait();
            game.chefWait();
            game.endTurn();
        }
        check("neglected customer left", c1.getStatus() == CustomerStatus.LEFT);
        check("left at turn 13 (100 / 8 rounds up)", game.getCurrentTurn() == 13);
        check("order cancelled", c1.getOrder().getStatus() == OrderStatus.CANCELLED);
        check("table freed", game.getRestaurant().getTables().get(0).isFree());
        check("no revenue from abandoned order", game.getStats().getRevenueCents() == 0);
        check("departure counted", game.getStats().getUnhappyDepartures() == 1);
    }

    private static void idleGameIsDefeat() {
        GameController game = GameController.createBaseGame();
        while (game.hasNextTurn()) {
            game.beginTurn();
            game.waiterWait();
            game.chefWait();
            game.endTurn();
        }
        check("always runs 18 turns", game.getCurrentTurn() == 18);
        check("idle manager loses", game.getResult() == GameResult.DEFEAT);
    }

    // ----- stage 2 tests -----

    private static void customerTypesBehaveDifferently() {
        Menu menu = Menu.createDefault();
        MenuItem burger = menu.findByName("Burger");
        PriceCalculator pricing = new PriceCalculator();
        Customer regular = new RegularCustomer(2, 1);
        Customer vip = new VIPCustomer(2, 1);
        Customer critic = new CriticCustomer(2, 1);
        check("regular pays full price", pricing.quote(regular, burger) == 1500);
        check("VIP pays 10% less", pricing.quote(vip, burger) == 1350);
        check("critic pays full price", pricing.quote(critic, burger) == 1500);
        check("regular loses 8", regular.getSatisfactionLossPerTurn() == 8);
        check("VIP loses 5", vip.getSatisfactionLossPerTurn() == 5);
        check("critic loses 12", critic.getSatisfactionLossPerTurn() == 12);
        check("customer 2 normally orders Burger", regular.chooseMenuItem(menu) == burger);
        check("critic always orders Pasta", critic.chooseMenuItem(menu).getName().equals("Pasta"));
        critic.loseSatisfactionForWaiting();
        check("critic loses 12 per waiting turn", critic.getSatisfaction() == 88);
    }

    /** A Critic served 2 turns after ordering gains 10; served 3 turns after, loses 10. */
    private static void criticReactsToServiceSpeed() {
        check("fast critic +10", criticSatisfactionAfterService(0) == 64 + 10);
        check("slow critic -10", criticSatisfactionAfterService(1) == 52 - 10);
    }

    /** The Critic arrives on turn 1 and orders on turn 2, then waits extraDelay more turns before being served. */
    private static int criticSatisfactionAfterService(int extraDelay) {
        ArrivalSchedule schedule = new ArrivalSchedule();
        schedule.add(1, CriticCustomer::new);
        GameController game = new GameController(GameConfig.base(), schedule, Menu.createDefault());
        game.beginTurn();
        Customer critic = game.getRestaurant().getWaitingQueue().get(0);
        game.waiterSeat(critic);
        game.chefWait();
        game.endTurn();                                   // 88
        game.beginTurn();
        game.waiterTakeOrder(critic);
        game.chefPrepare(critic.getOrder());
        game.endTurn();                                   // 76
        game.beginTurn();
        game.waiterWait();
        game.chefPrepare(critic.getOrder());
        game.endTurn();                                   // 64
        for (int i = 0; i < extraDelay; i++) {
            game.beginTurn();
            game.waiterWait();
            game.chefWait();
            game.endTurn();                               // 52
        }
        game.beginTurn();
        game.waiterServe(critic.getOrder());
        return critic.getSatisfaction();
    }

    // ----- stage 3 tests -----

    private static void comboMealIsSumOfComponents() {
        Menu menu = Menu.createDefault();
        ComboMeal combo = new ComboMeal(menu.findByName("Salad"), menu.findByName("Pasta"));
        check("combo price 90% of $27 = $24.30", combo.getPriceCents() == 2430);
        check("combo units 1 + 2 = 3", combo.getPrepUnits() == 3);
        check("combo has two components", combo.getComponents().size() == 2);
        check("menu has 5 items", menu.getItems().size() == 5);
        check("customer 4 picks Salad + Burger combo", menu.itemForArrival(4).getPriceCents() == 2160);
        check("customer 5 picks Salad + Pasta combo", menu.itemForArrival(5).getPriceCents() == 2430);
    }

    /** VIP Burger in Happy Hour = $10.80; price is fixed; once per game; lasts 2 turns. */
    private static void happyHourPricingAndLimits() {
        ArrivalSchedule schedule = new ArrivalSchedule();
        schedule.add(1, VIPCustomer::new);
        schedule.add(2, RegularCustomer::new);
        Menu burgerOnly = new Menu();
        burgerOnly.add(new MenuItem("Burger", 1500, 1));
        GameController game = new GameController(GameConfig.base(), schedule, burgerOnly);

        game.beginTurn();                                          // turn 1
        Customer vip = game.getRestaurant().getWaitingQueue().get(0);
        check("activate Happy Hour", game.activateHappyHour().isSuccess());
        check("active, 2 turns left", game.getHappyHour().getRemainingTurns() == 2);
        check("cannot activate twice", !game.activateHappyHour().isSuccess());
        game.hostSeat(vip);
        game.waiterTakeOrder(vip);
        check("VIP burger in Happy Hour = $10.80", vip.getOrder().getPriceCents() == 1080);
        game.chefPrepare(vip.getOrder());
        game.endTurn();

        game.beginTurn();                                          // turn 2: still active
        check("still active on turn 2", game.getHappyHour().isActive()
                && game.getHappyHour().getRemainingTurns() == 1);
        Customer regular = game.getRestaurant().getWaitingQueue().get(0);
        check("arrival capped at 100 after recovery", regular.getSatisfaction() == 100);
        game.hostSeat(regular);
        game.waiterServe(vip.getOrder());
        game.chefWait();
        game.endTurn();
        check("Happy Hour over after 2 turns", !game.getHappyHour().isActive());

        game.beginTurn();                                          // turn 3: regular orders, no discount
        check("recovery stops after Happy Hour", regular.getSatisfaction() == 92);
        game.waiterTakeOrder(regular);
        check("regular burger after Happy Hour = $15.00", regular.getOrder().getPriceCents() == 1500);
        check("VIP price stayed locked", vip.getOrder().getPriceCents() == 1080);
        check("activation refused after staff act", !game.activateHappyHour().isSuccess());
    }

    private static void hostAndWaiterCanBothSeat() {
        ArrivalSchedule schedule = new ArrivalSchedule();
        schedule.add(1, RegularCustomer::new);
        schedule.add(2, RegularCustomer::new);
        schedule.add(3, RegularCustomer::new);
        GameController game = new GameController(GameConfig.base(), schedule, Menu.createDefault());
        game.beginTurn();
        game.hostWait();
        game.waiterWait();
        game.chefWait();
        game.endTurn();
        game.beginTurn();                                          // C1 and C2 queued
        List<Customer> queue = game.getRestaurant().getWaitingQueue();
        Customer c1 = queue.get(0);
        Customer c2 = queue.get(1);
        check("host seats C1", game.hostSeat(c1).isSuccess());
        check("host only once per turn", !game.hostSeat(c2).isSuccess());
        check("waiter also seats C2", game.waiterSeat(c2).isSuccess());
        game.chefWait();
        game.endTurn();
        game.beginTurn();                                          // C3 arrives, no free table
        Customer c3 = game.getRestaurant().getWaitingQueue().get(0);
        ActionResult full = game.hostSeat(c3);
        check("no free table is refused", !full.isSuccess());
        check("refusal keeps host available", game.isHostAvailable());
    }

    // ----- helpers -----

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failures.add(name);
        }
    }
}
