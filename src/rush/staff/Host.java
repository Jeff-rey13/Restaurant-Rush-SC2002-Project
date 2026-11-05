package rush.staff;

import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.Restaurant;

/**
 * The Host seats at most one queued customer per turn, on top of the Waiter's
 * task. The Host acts first each turn (at the door), so the Waiter can take the
 * order of a customer the Host just seated.
 */
public class Host extends Staff {

    public Host() {
        super("Host");
    }

    @Override
    public String describeDuties() {
        return "seat one queued customer (in addition to the Waiter)";
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
}
