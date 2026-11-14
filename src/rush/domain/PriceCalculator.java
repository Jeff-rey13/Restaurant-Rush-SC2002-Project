package rush.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out the bill when an order is taken. This is the only place that does it.
 * First the customer's own discount is applied (e.g. VIP 10% off), then each
 * restaurant-wide pricing policy (e.g. Happy Hour 20% off), rounding to the
 * nearest cent after each step. The final price is stored in the Order.
 */
public class PriceCalculator {

    private final List<PricingPolicy> policies = new ArrayList<>();

    public void addPolicy(PricingPolicy policy) {
        policies.add(policy);
    }

    /** Price in cents that this customer will pay for this item. */
    public int quote(Customer customer, MenuItem item) {
        // Each customer type applies its own discount (e.g. VIP 10% off).
        int price = customer.adjustBill(item.getPriceCents());
        for (PricingPolicy policy : policies) {
            price = policy.apply(price);
        }
        return price;
    }
}
