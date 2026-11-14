package rush.domain;

/**
 * A customer who visits the restaurant. Subclasses define how quickly the
 * customer loses patience. All customer types follow the same order and
 * service flow, which is enforced here: the status field can only change
 * through the methods below, so no outside class can, for example, make a
 * customer pay before being served.
 */
public abstract class Customer {

    public static final int MAX_SATISFACTION = 100;

    private final int number;
    private final int arrivalTurn;

    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Table table;
    private Order order;
    private int servedTurn = -1;
    private int readyToPayTurn = -1;
    private int paidSatisfaction = -1;

    protected Customer(int number, int arrivalTurn) {
        this.number = number;
        this.arrivalTurn = arrivalTurn;
    }

    /** Display name of the customer type, e.g. "Regular". */
    public abstract String getTypeName();

    /** Satisfaction lost at the end of each turn spent waiting (not yet served). */
    public abstract int getSatisfactionLossPerTurn();

    /** The menu item this customer orders. Default: the documented cycling rule. */
    public MenuItem chooseMenuItem(Menu menu) {
        return menu.itemForArrival(number);
    }

    // ----- queries -----

    public String getLabel() {
        return "C" + number;
    }

    public int getNumber() {
        return number;
    }

    public int getArrivalTurn() {
        return arrivalTurn;
    }

    public int getSatisfaction() {
        return satisfaction;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public Table getTable() {
        return table;
    }

    public Order getOrder() {
        return order;
    }

    public int getReadyToPayTurn() {
        return readyToPayTurn;
    }

    /** Satisfaction recorded at the moment of payment (-1 if not paid). */
    public int getPaidSatisfaction() {
        return paidSatisfaction;
    }

    /** True for customers who have not received their food yet (queue, seated or ordered). */
    public boolean isWaitingForService() {
        return status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED
                || status == CustomerStatus.ORDERED;
    }

    /** True while the customer is still inside the restaurant. */
    public boolean isPresent() {
        return status != CustomerStatus.PAID && status != CustomerStatus.LEFT;
    }

    // ----- state transitions -----

    void seatAt(Table newTable) {
        requireStatus(CustomerStatus.WAITING);
        newTable.assign(this);
        this.table = newTable;
        status = CustomerStatus.SEATED;
    }

    public void placeOrder(Order newOrder) {
        requireStatus(CustomerStatus.SEATED);
        this.order = newOrder;
        status = CustomerStatus.ORDERED;
    }

    public void receiveFood(int turn) {
        requireStatus(CustomerStatus.ORDERED);
        status = CustomerStatus.EATING;
        servedTurn = turn;
    }

    /** A customer served on an earlier turn finishes eating and becomes READY_TO_PAY. */
    public boolean finishEatingIfDone(int turn) {
        if (status == CustomerStatus.EATING && servedTurn < turn) {
            status = CustomerStatus.READY_TO_PAY;
            readyToPayTurn = turn;
            return true;
        }
        return false;
    }

    /** A customer who became READY_TO_PAY this turn pays next turn at the earliest. */
    public boolean canPay(int turn) {
        return status == CustomerStatus.READY_TO_PAY && readyToPayTurn < turn;
    }

    /** Pays the bill: records satisfaction, frees the table. Returns the amount paid. */
    public int pay() {
        requireStatus(CustomerStatus.READY_TO_PAY);
        paidSatisfaction = satisfaction;
        status = CustomerStatus.PAID;
        order.markPaid();
        leaveTable();
        return order.getPriceCents();
    }

    /**
     * End-of-turn waiting penalty. Returns true if satisfaction has reached 0,
     * meaning the customer is about to leave.
     */
    public boolean loseSatisfactionForWaiting() {
        setSatisfaction(satisfaction - getSatisfactionLossPerTurn());
        return satisfaction == 0;
    }

    /** Leaves without paying: cancels any unfinished order and frees the table. */
    public void leaveUnhappy() {
        if (order != null) {
            order.cancel();
        }
        status = CustomerStatus.LEFT;
        leaveTable();
    }

    private void leaveTable() {
        if (table != null) {
            table.release();
            table = null;
        }
    }

    private void setSatisfaction(int value) {
        satisfaction = Math.max(0, Math.min(MAX_SATISFACTION, value));
    }

    private void requireStatus(CustomerStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(getLabel() + " is " + status + ", expected " + expected);
        }
    }
}
