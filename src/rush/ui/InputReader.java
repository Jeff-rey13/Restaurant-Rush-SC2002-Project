package rush.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Reads numbered menu choices. Anything that is not a number or is out of range
 * is rejected with a message and the question is asked again.
 *
 * Lines starting with '#' are skipped, so test scripts can have comments.
 * With echo on, each answer is printed, so a piped run gives a readable transcript.
 */
public class InputReader {

    private final BufferedReader in;
    private final PrintStream out;
    private final boolean echo;

    public InputReader(InputStream in, PrintStream out, boolean echo) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = out;
        this.echo = echo;
    }

    /** Keeps asking until the user enters a whole number from min to max. */
    public int readChoice(String prompt, int min, int max) {
        while (true) {
            out.print(prompt + " [" + min + "-" + max + "]: ");
            String line = nextLine();
            if (echo) {
                out.println(line);
            }
            String text = line.trim();
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // go on to the error message below
            }
            out.println("  Invalid input '" + text + "'. Please enter a number from " + min + " to " + max + ".");
        }
    }

    private String nextLine() {
        try {
            String line;
            do {
                line = in.readLine();
                if (line == null) {
                    throw new InputEndedException();
                }
            } while (line.trim().startsWith("#"));
            return line;
        } catch (IOException e) {
            throw new InputEndedException();
        }
    }

    /** Thrown when standard input is closed (e.g. at the end of a test script). */
    public static class InputEndedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public InputEndedException() {
            super("Input ended");
        }
    }
}
