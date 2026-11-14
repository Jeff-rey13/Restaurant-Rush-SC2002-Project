package rush.control;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import rush.domain.Customer;
import rush.domain.RegularCustomer;

/**
 * Which kind of customer arrives on which turn. Each entry is a factory
 * (arrival number, turn) -> Customer, so the controller never needs to know
 * concrete customer classes.
 */
public class ArrivalSchedule {

    private final Map<Integer, BiFunction<Integer, Integer, Customer>> arrivals = new TreeMap<>();
    private int arrivalsSoFar;

    /** Base setting: one Regular customer at the start of turns 1, 4, 7, 10, 13 and 16. */
    public static ArrivalSchedule base() {
        ArrivalSchedule schedule = new ArrivalSchedule();
        schedule.add(1, RegularCustomer::new);
        schedule.add(4, RegularCustomer::new);
        schedule.add(7, RegularCustomer::new);
        schedule.add(10, RegularCustomer::new);
        schedule.add(13, RegularCustomer::new);
        schedule.add(16, RegularCustomer::new);
        return schedule;
    }

    public void add(int turn, BiFunction<Integer, Integer, Customer> factory) {
        arrivals.put(turn, factory);
    }

    /** Creates the customer scheduled for this turn, or returns null if nobody arrives. */
    public Customer arrivalFor(int turn) {
        BiFunction<Integer, Integer, Customer> factory = arrivals.get(turn);
        if (factory == null) {
            return null;
        }
        arrivalsSoFar++;
        return factory.apply(arrivalsSoFar, turn);
    }

    public int getTotalArrivals() {
        return arrivals.size();
    }
}
