package rush.domain;

/** The stages of an order. */
public enum OrderStatus {
    /** Taken by the Waiter; the Chef still has preparation units to do. */
    IN_PREPARATION,
    /** All preparation units done; waiting for the Waiter to serve it. */
    READY,
    /** Delivered to the customer's table. */
    SERVED,
    /** The customer has paid the bill. */
    PAID,
    /** The customer left before the order was served. */
    CANCELLED
}
