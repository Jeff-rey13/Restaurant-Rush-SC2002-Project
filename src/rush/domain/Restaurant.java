package rush.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The restaurant floor: menu, tables, waiting queue, every customer who has
 * arrived and every order taken. It owns all the other domain objects.
 */
public class Restaurant {

    private final Menu menu;
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> waitingQueue = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private int nextOrderId = 1;

    public Restaurant(Menu menu, int tableCount) {
        this.menu = menu;
        for (int i = 1; i <= tableCount; i++) {
            tables.add(new Table(i));
        }
    }

    public Menu getMenu() {
        return menu;
    }

    /** A newly arrived customer joins the end of the waiting queue. */
    public void admit(Customer customer) {
        customers.add(customer);
        waitingQueue.add(customer);
    }

    /**
     * Seats a queued customer at the lowest-numbered free table. Used by every
     * role that is allowed to seat customers.
     */
    public ActionResult seatCustomer(Customer customer) {
        if (!waitingQueue.contains(customer)) {
            return ActionResult.fail(customer.getLabel() + " is not in the waiting queue.");
        }
        Table table = findFreeTable();
        if (table == null) {
            return ActionResult.fail("No free table: all " + tables.size()
                    + " tables are occupied until a customer pays or leaves.");
        }
        waitingQueue.remove(customer);
        customer.seatAt(table);
        return ActionResult.ok(customer.getLabel() + " is seated at " + table.getLabel() + ".");
    }

    public Order createOrder(Customer customer, MenuItem item, int priceCents, int turn) {
        Order order = new Order(nextOrderId++, customer, item, priceCents, turn);
        orders.add(order);
        return order;
    }

    /** The customer leaves unhappy: removed from the queue, order cancelled, table freed. */
    public void removeUnhappyCustomer(Customer customer) {
        waitingQueue.remove(customer);
        customer.leaveUnhappy();
    }

    public Table findFreeTable() {
        for (Table table : tables) {
            if (table.isFree()) {
                return table;
            }
        }
        return null;
    }

    public boolean hasFreeTable() {
        return findFreeTable() != null;
    }

    public List<Table> getTables() {
        return Collections.unmodifiableList(tables);
    }

    public List<Customer> getWaitingQueue() {
        return Collections.unmodifiableList(waitingQueue);
    }

    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    /** Customers currently sitting at a table, in table order. */
    public List<Customer> getSeatedCustomers() {
        List<Customer> seated = new ArrayList<>();
        for (Table table : tables) {
            if (!table.isFree()) {
                seated.add(table.getOccupant());
            }
        }
        return seated;
    }

    /** Orders still needing the Chef or the Waiter. */
    public List<Order> getActiveOrders() {
        List<Order> active = new ArrayList<>();
        for (Order order : orders) {
            if (order.isActive()) {
                active.add(order);
            }
        }
        return active;
    }

    /** Customers who are still waiting for a table or for their food. */
    public List<Customer> getCustomersWaitingForService() {
        List<Customer> waiting = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.isWaitingForService()) {
                waiting.add(customer);
            }
        }
        return waiting;
    }

    /** Customers allowed to pay this turn, oldest READY_TO_PAY first. */
    public List<Customer> getCustomersAbleToPay(int turn) {
        List<Customer> ready = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.canPay(turn)) {
                ready.add(customer);
            }
        }
        ready.sort(Comparator.comparingInt(Customer::getReadyToPayTurn)
                .thenComparingInt(Customer::getNumber));
        return ready;
    }
}
