package rush.gui;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import rush.control.GameController;

/**
 * Starts the game window. Options:
 *   --replay tests/stage1-victory.txt   play a test script automatically
 *   --speed 2                           replay speed
 *   --mute                              start with sound and music off
 *   --start                             skip the title screen
 */
public final class GuiMain {

    private GuiMain() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("No display available. Use the command-line version: java -cp out rush.Main");
            return;
        }
        String replayFile = null;
        double speed = 1;
        boolean mute = false;
        boolean skipTitle = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--replay") && i + 1 < args.length) {
                replayFile = args[++i];
            } else if (args[i].equals("--speed") && i + 1 < args.length) {
                speed = Double.parseDouble(args[++i]);
            } else if (args[i].equals("--mute")) {
                mute = true;
            } else if (args[i].equals("--start")) {
                skipTitle = true;
            }
        }
        String replay = replayFile;
        double replaySpeed = speed;
        boolean startMuted = mute;
        boolean start = skipTitle;
        SwingUtilities.invokeLater(() -> open(replay, replaySpeed, startMuted, start));
    }

    private static void open(String replayFile, double speed, boolean mute, boolean skipTitle) {
        JFrame frame = new JFrame("Restaurant Rush");
        GamePanel panel = new GamePanel(GameController::createBaseGame);
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(icon());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Music.stop();
            }
        });
        frame.pack();
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if (frame.getWidth() > screen.width || frame.getHeight() > screen.height) {
            double s = Math.min(screen.width * 0.95 / frame.getWidth(), screen.height * 0.95 / frame.getHeight());
            frame.setSize((int) (frame.getWidth() * s), (int) (frame.getHeight() * s));
        }
        frame.setMinimumSize(new Dimension(800, 480));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
        if (mute) {
            SoundFx.setEnabled(false);
            Music.toggle();
        } else {
            Music.start();
        }
        if (skipTitle && replayFile == null) {
            panel.startGame();
        }
        if (replayFile != null) {
            try {
                Path path = Paths.get(replayFile);
                panel.startReplay(ScriptedPlayer.load(path), path.getFileName().toString(), speed);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Could not read the replay file: " + replayFile);
            }
        }
    }

    private static BufferedImage icon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(32, 40);
        g.scale(1.4, 1.4);
        DishPainter.paintDish(g, "Burger");
        g.dispose();
        return img;
    }
}
