package rush.control;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import rush.domain.CriticCustomer;
import rush.domain.Customer;
import rush.domain.RegularCustomer;
import rush.domain.VIPCustomer;

/**
 * Decides which type of customer arrives on which turn. Each entry builds the
 * customer from (arrival number, turn), so the controller does not need to know
 * the customer classes.
 */
public class ArrivalSchedule {

    private final Map<Integer, BiFunction<Integer, Integer, Customer>> arrivals = new TreeMap<>();
    private int arrivalsSoFar;

    /**
     * Base setting: one customer arrives at the start of turns 1, 4, 7, 10, 13 and 16.
     * Stage 2 swaps two Regular customers for a Critic (turn 4) and a VIP (turn 7).
     */
    public static ArrivalSchedule base() {
        ArrivalSchedule schedule = new ArrivalSchedule();
        schedule.add(1, RegularCustomer::new);
        schedule.add(4, CriticCustomer::new);
        schedule.add(7, VIPCustomer::new);
        schedule.add(10, RegularCustomer::new);
        schedule.add(13, RegularCustomer::new);
        schedule.add(16, RegularCustomer::new);
        return schedule;
    }

    public void add(int turn, BiFunction<Integer, Integer, Customer> factory) {
        arrivals.put(turn, factory);
    }

    /** Creates the customer for this turn, or returns null if nobody comes. */
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
