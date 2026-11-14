package rush.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import rush.domain.Customer;
import rush.domain.Order;
import rush.domain.Restaurant;

/**
 * Plays a command-line test script (tests/*.txt) in the game window, reading the
 * answers exactly as ConsoleUI would, so the window shows the same run as the
 * saved transcript. Useful for an accelerated replay in the video.
 */
final class ScriptedPlayer {

    private final List<String> answers;
    private int next;

    ScriptedPlayer(List<String> lines) {
        answers = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().startsWith("#")) {
                answers.add(line.trim());
            }
        }
    }

    static ScriptedPlayer load(Path file) throws IOException {
        return new ScriptedPlayer(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    /** Performs one manager step; returns false when the script has run out. */
    boolean step(TurnFlow flow) {
        try {
            if (flow.phase() == TurnFlow.Phase.WAITER) {
                waiterStep(flow);
            } else if (flow.phase() == TurnFlow.Phase.CHEF) {
                List<Order> orders = flow.game().getRestaurant().getActiveOrders();
                int choice = nextChoice(0, orders.size());
                flow.choose(choice == 0 ? flow.waitOption(TurnFlow.Role.CHEF) : flow.cook(orders.get(choice - 1)));
            }
            return true;
        } catch (IllegalStateException endOfScript) {
            return false;
        }
    }

    private void waiterStep(TurnFlow flow) {
        Restaurant r = flow.game().getRestaurant();
        int task = nextChoice(0, 3);
        if (task == 0) {
            flow.choose(flow.waitOption(TurnFlow.Role.WAITER));
        } else if (task == 1) {
            Customer c = pick(r.getWaitingQueue());
            if (c != null) {
                flow.choose(flow.seat(c));
            }
        } else if (task == 2) {
            Customer c = pick(r.getSeatedCustomers());
            if (c != null) {
                flow.choose(flow.takeOrder(c));
            }
        } else {
            List<Order> orders = r.getActiveOrders();
            if (!orders.isEmpty()) {
                int choice = nextChoice(0, orders.size());
                if (choice > 0) {
                    flow.choose(flow.serve(orders.get(choice - 1)));
                }
            }
        }
    }

    /** Like ConsoleUI: an empty list asks for nothing; 0 means "back". */
    private Customer pick(List<Customer> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        int choice = nextChoice(0, candidates.size());
        return choice == 0 ? null : candidates.get(choice - 1);
    }

    /** Like InputReader: skips answers that are not numbers in range. */
    int nextChoice(int min, int max) {
        while (next < answers.size()) {
            String text = answers.get(next++);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // invalid answer: the console would ask again, so read the next one
            }
        }
        throw new IllegalStateException("script finished");
    }
}
