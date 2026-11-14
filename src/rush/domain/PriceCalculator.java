package rush.domain;

/**
 * Single place where a bill is worked out when an order is taken.
 * The returned price is locked into the Order.
 */
public class PriceCalculator {

    /** Price in cents that this customer will pay for this item. */
    public int quote(Customer customer, MenuItem item) {
        // Customer-type discount is dispatched polymorphically (e.g. VIP 10% off).
        return customer.adjustBill(item.getPriceCents());
    }
}
