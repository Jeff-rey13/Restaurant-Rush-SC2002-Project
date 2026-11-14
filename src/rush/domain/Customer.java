package rush.domain;

/**
 * A customer in the restaurant. Subclasses set how fast the customer loses
 * patience, and can change the bill, the food choice and the reaction to
 * service. Every customer type goes through the same steps, and this class
 * enforces them: the status only changes through the methods below, so no
 * other class can, for example, make a customer pay before being served.
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

    /** Satisfaction lost for each turn spent waiting for food. */
    public abstract int getSatisfactionLossPerTurn();

    /** The menu item this customer orders. By default it follows the menu cycling rule. */
    public MenuItem chooseMenuItem(Menu menu) {
        return menu.itemForArrival(number);
    }

    /** One-line description of how this customer type behaves, shown on arrival. */
    public String describeBehaviour() {
        return "loses " + getSatisfactionLossPerTurn() + " satisfaction per waiting turn";
    }

    /** The bill this customer type pays for a full price. By default, the full price. */
    public int adjustBill(int fullPriceCents) {
        return fullPriceCents;
    }

    /**
     * Called once when the food arrives. {@code turnsSinceOrder} is how many turns
     * passed between taking the order and serving it. Subclasses can change
     * satisfaction here and return a message for the manager, or null.
     * By default there is no reaction.
     */
    protected String reactToService(int turnsSinceOrder) {
        return null;
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

    /** True if the customer has not got their food yet (in the queue, seated or ordered). */
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

    /**
     * The food arrives and the customer starts eating. This is final so every
     * customer type follows the same steps, but each type can react through
     * reactToService. Returns the reaction message, or null.
     */
    public final String receiveFood(int turn) {
        requireStatus(CustomerStatus.ORDERED);
        status = CustomerStatus.EATING;
        servedTurn = turn;
        return reactToService(turn - order.getTakenTurn());
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

    /** Pays the bill, records satisfaction and frees the table. Returns the amount paid. */
    public int pay() {
        requireStatus(CustomerStatus.READY_TO_PAY);
        paidSatisfaction = satisfaction;
        status = CustomerStatus.PAID;
        order.markPaid();
        leaveTable();
        return order.getPriceCents();
    }

    /**
     * Applies the waiting penalty at the end of the turn. Returns true if
     * satisfaction is now 0, which means the customer is about to leave.
     */
    public boolean loseSatisfactionForWaiting() {
        setSatisfaction(satisfaction - getSatisfactionLossPerTurn());
        return satisfaction == 0;
    }

    /** Leaves without paying. Cancels any unfinished order and frees the table. */
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

    /** Recovery from Happy Hour. Only customers who are still waiting get it. */
    public void recoverSatisfaction(int points) {
        if (isWaitingForService()) {
            setSatisfaction(satisfaction + points);
        }
    }

    /** Lets subclasses raise or lower satisfaction. It always stays between 0 and 100. */
    protected void adjustSatisfaction(int delta) {
        setSatisfaction(satisfaction + delta);
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
