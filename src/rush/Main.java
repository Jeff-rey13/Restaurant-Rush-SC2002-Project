package rush;

import java.util.Arrays;

import rush.control.GameController;
import rush.ui.ConsoleUI;
import rush.ui.InputReader;

/**
 * Starts the console game. Run with:  java -cp out rush.Main
 * Add --echo when piping in a test script so the answers show up in the output.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean echo = Arrays.asList(args).contains("--echo");
        GameController game = GameController.createBaseGame();
        InputReader input = new InputReader(System.in, System.out, echo);
        new ConsoleUI(game, input, System.out).run();
    }
}
