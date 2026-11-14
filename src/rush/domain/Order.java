package rush.domain;

/**
 * One customer's order. The price is fixed when the order is taken.
 * The order keeps track of its own cooking progress and status.
 */
public class Order {

    private final int id;
    private final Customer customer;
    private final MenuItem item;
    private final int priceCents;
    private final int takenTurn;

    private int unitsDone;
    private OrderStatus status = OrderStatus.IN_PREPARATION;
    private int readyTurn = -1;

    public Order(int id, Customer customer, MenuItem item, int priceCents, int takenTurn) {
        this.id = id;
        this.customer = customer;
        this.item = item;
        this.priceCents = priceCents;
        this.takenTurn = takenTurn;
    }

    public String getLabel() {
        return "Order #" + id;
    }

    public int getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public MenuItem getItem() {
        return item;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getTakenTurn() {
        return takenTurn;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public int getUnitsDone() {
        return unitsDone;
    }

    public int getRemainingUnits() {
        return item.getPrepUnits() - unitsDone;
    }

    /** e.g. "1/2 units". */
    public String getProgress() {
        return unitsDone + "/" + item.getPrepUnits() + " units";
    }

    /** True while the order still needs the Chef or the Waiter. */
    public boolean isActive() {
        return status == OrderStatus.IN_PREPARATION || status == OrderStatus.READY;
    }

    /** The Chef does one preparation unit. The order is READY after the last unit. */
    public void prepareOneUnit(int turn) {
        if (status != OrderStatus.IN_PREPARATION) {
            throw new IllegalStateException(getLabel() + " is not in preparation");
        }
        unitsDone++;
        if (unitsDone >= item.getPrepUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
        }
    }

    /** A dish can only be served if it was already READY before the Waiter acts this turn. */
    public boolean isReadyToServe(int turn) {
        return status == OrderStatus.READY && readyTurn < turn;
    }

    public void markServed() {
        if (status != OrderStatus.READY) {
            throw new IllegalStateException(getLabel() + " is not READY");
        }
        status = OrderStatus.SERVED;
    }

    public void markPaid() {
        if (status != OrderStatus.SERVED) {
            throw new IllegalStateException(getLabel() + " has not been served");
        }
        status = OrderStatus.PAID;
    }

    /** Cancels the order if it was never served (the customer left unhappy). */
    public void cancel() {
        if (isActive()) {
            status = OrderStatus.CANCELLED;
        }
    }
}
