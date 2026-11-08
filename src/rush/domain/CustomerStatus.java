package rush.domain;

/** The stages a customer goes through, from arriving to leaving. */
public enum CustomerStatus {
    /** In the waiting queue, no table yet. */
    WAITING,
    /** Seated at a table, order not yet taken. */
    SEATED,
    /** Order taken, waiting for the food. */
    ORDERED,
    /** Food served, customer is eating. */
    EATING,
    /** Finished eating, waiting for the Cashier. */
    READY_TO_PAY,
    /** Paid and left happily. */
    PAID,
    /** Satisfaction hit 0, so the customer left without paying. */
    LEFT
}
