package rush;

import java.util.ArrayList;
import java.util.List;

import rush.control.GameController;
import rush.control.GameResult;
import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.OrderStatus;

/**
 * Plain-Java rule tests (no external libraries, so they run offline with javac only).
 * Run with ./run-tests.sh. Each test drives the controller directly, without the UI,
 * which also shows that the rules do not live in the UI.
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

        System.out.println(passed + " checks passed, " + failures.size() + " failed.");
        for (String failure : failures) {
            System.out.println("  FAILED: " + failure);
        }
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ tests

    private static void moneyIsRoundedToNearestCent() {
        check("Salad+Pasta combo price", Money.percentOf(900 + 1800, 90) == 2430);
        check("VIP burger in Happy Hour", Money.percentOf(Money.percentOf(1500, 90), 80) == 1080);
        check("format", Money.format(2430).equals("$24.30"));
        check("format small", Money.format(5).equals("$0.05"));
    }

    /** Brief 3.3: Pasta customer, full attention: 92, 84, 76, served, ready to pay, pays 76. */
    private static void exampleFromBriefSection33() {
        GameController game = GameController.createBaseGame();
        // In the base game the first Pasta customer is C3 (arrives turn 7), so we play
        // turns 1-6 normally and then check the brief's six-turn timeline on C3.
        playFullAttentionUntil(game, 6);
        game.beginTurn();                  // turn 7: C3 arrives
        Customer c3 = game.getRestaurant().getWaitingQueue().get(0);
        game.waiterSeat(c3);
        game.chefWait();
        game.endTurn();
        check("3.3 turn 1 -> 92", c3.getSatisfaction() == 92);

        game.beginTurn();                  // turn 8
        game.waiterTakeOrder(c3);
        Order pasta = c3.getOrder();
        check("C3 orders Pasta", pasta.getItem().getName().equals("Pasta"));
        game.chefPrepare(pasta);
        game.endTurn();
        check("3.3 turn 2 -> 84", c3.getSatisfaction() == 84);

        game.beginTurn();                  // turn 9
        ActionResult early = game.waiterServe(pasta);
        check("cannot serve unfinished dish", !early.isSuccess());
        game.waiterWait();
        game.chefPrepare(pasta);
        check("pasta READY after 2 units", pasta.getStatus() == OrderStatus.READY);
        game.endTurn();
        check("3.3 turn 3 -> 76", c3.getSatisfaction() == 76);

        game.beginTurn();                  // turn 10
        check("serve READY pasta", game.waiterServe(pasta).isSuccess());
        game.chefWait();
        game.endTurn();
        check("3.3 turn 4 no further loss", c3.getSatisfaction() == 76);

        game.beginTurn();                  // turn 11
        game.waiterWait();
        game.chefWait();
        game.endTurn();
        check("3.3 turn 5 READY_TO_PAY", c3.getStatus() == CustomerStatus.READY_TO_PAY);

        int revenueBefore = game.getStats().getRevenueCents();
        game.beginTurn();                  // turn 12
        game.waiterWait();
        game.chefWait();
        game.endTurn();
        check("3.3 turn 6 PAID", c3.getStatus() == CustomerStatus.PAID);
        check("3.3 recorded satisfaction 76", c3.getPaidSatisfaction() == 76);
        check("3.3 revenue +$18", game.getStats().getRevenueCents() - revenueBefore == 1800);
        check("3.3 table freed", c3.getTable() == null);
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
        GameController game = GameController.createBaseGame();
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

    // ---------------------------------------------------------------- helpers

    /**
     * Plays turns 1..lastTurn with the "full attention" policy for 1-unit dishes:
     * serve if possible, else take an order, else seat; Chef cooks the oldest order.
     */
    private static void playFullAttentionUntil(GameController game, int lastTurn) {
        while (game.getCurrentTurn() < lastTurn) {
            game.beginTurn();
            boolean done = false;
            for (Order order : game.getRestaurant().getActiveOrders()) {
                if (!done && order.isReadyToServe(game.getCurrentTurn())) {
                    done = game.waiterServe(order).isSuccess();
                }
            }
            for (Customer customer : game.getRestaurant().getSeatedCustomers()) {
                if (!done && customer.getOrder() == null) {
                    done = game.waiterTakeOrder(customer).isSuccess();
                }
            }
            if (!done && !game.getRestaurant().getWaitingQueue().isEmpty()) {
                done = game.waiterSeat(game.getRestaurant().getWaitingQueue().get(0)).isSuccess();
            }
            if (!done) {
                game.waiterWait();
            }
            boolean cooked = false;
            for (Order order : game.getRestaurant().getActiveOrders()) {
                if (!cooked && order.getStatus() == OrderStatus.IN_PREPARATION) {
                    cooked = game.chefPrepare(order).isSuccess();
                }
            }
            if (!cooked) {
                game.chefWait();
            }
            game.endTurn();
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failures.add(name);
        }
    }
}
