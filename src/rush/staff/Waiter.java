package rush.staff;

import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.MenuItem;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.OrderStatus;
import rush.domain.PriceCalculator;
import rush.domain.Restaurant;

/** The Waiter does one task per turn: seat a customer, take an order or serve a READY dish. */
public class Waiter extends Staff {

    public Waiter() {
        super("Waiter");
    }

    @Override
    public String describeDuties() {
        return "seat a queued customer, take an order, or serve a READY dish (one task)";
    }

    public ActionResult seat(Customer customer, Restaurant restaurant) {
        if (!isAvailable()) {
            return alreadyBusy();
        }
        ActionResult result = restaurant.seatCustomer(customer);
        if (result.isSuccess()) {
            markBusy();
        }
        return result;
    }

    public ActionResult takeOrder(Customer customer, Restaurant restaurant,
                                  PriceCalculator pricing, int turn) {
        if (!isAvailable()) {
            return alreadyBusy();
        }
        if (customer.getStatus() == CustomerStatus.WAITING) {
            return ActionResult.fail(customer.getLabel() + " is still in the queue and must be seated first.");
        }
        if (customer.getStatus() != CustomerStatus.SEATED) {
            return ActionResult.fail(customer.getLabel() + " has already ordered.");
        }
        MenuItem item = customer.chooseMenuItem(restaurant.getMenu());
        int price = pricing.quote(customer, item);
        Order order = restaurant.createOrder(customer, item, price, turn);
        customer.placeOrder(order);
        markBusy();
        return ActionResult.ok(customer.getLabel() + " (" + customer.getTypeName() + ") orders "
                + item.getName() + ": " + order.getLabel() + ", bill " + Money.format(price)
                + ", needs " + item.getPrepUnits() + " preparation unit(s).");
    }

    public ActionResult serve(Order order, int turn) {
        if (!isAvailable()) {
            return alreadyBusy();
        }
        if (order.getStatus() == OrderStatus.IN_PREPARATION) {
            return ActionResult.fail(order.getLabel() + " is not READY yet ("
                    + order.getProgress() + " done). The Chef must finish it first.");
        }
        if (!order.isReadyToServe(turn)) {
            return ActionResult.fail(order.getLabel() + " cannot be served (status "
                    + order.getStatus() + ").");
        }
        order.markServed();
        String reaction = order.getCustomer().receiveFood(turn);
        markBusy();
        String message = order.getLabel() + " (" + order.getItem().getName() + ") served to "
                + order.getCustomer().getLabel() + ". Status: SERVED.";
        return ActionResult.ok(reaction == null ? message : message + " " + reaction);
    }
}
