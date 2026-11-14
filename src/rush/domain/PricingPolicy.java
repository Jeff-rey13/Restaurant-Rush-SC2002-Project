package rush.domain;

/**
 * A restaurant-wide pricing rule applied after the customer's own discount
 * when an order is taken (e.g. Happy Hour). PriceCalculator only uses this
 * interface, so new promotions can be added without changing it.
 */
public interface PricingPolicy {

    /** Returns the new price, or the same price if the policy is not active now. */
    int apply(int priceCents);
}
