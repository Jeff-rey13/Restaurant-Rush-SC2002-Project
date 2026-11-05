package rush.staff;

import java.util.List;

import rush.domain.Customer;
import rush.domain.Restaurant;

/** The Cashier automatically charges at most one READY_TO_PAY customer per turn, oldest first. */
public class Cashier extends Staff {

    public Cashier() {
        super("Cashier");
    }

    @Override
    public String describeDuties() {
        return "automatically charges one READY_TO_PAY customer (oldest first)";
    }

    /**
     * Charges the oldest customer who can pay this turn.
     * Returns that customer (now PAID), or null if nobody could pay.
     */
    public Customer collectPayment(Restaurant restaurant, int turn) {
        if (!isAvailable()) {
            return null;
        }
        List<Customer> payers = restaurant.getCustomersAbleToPay(turn);
        if (payers.isEmpty()) {
            return null;
        }
        Customer customer = payers.get(0);
        customer.pay();
        markBusy();
        return customer;
    }
}
