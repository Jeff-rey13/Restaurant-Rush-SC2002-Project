package rush.gui;

import static rush.gui.UiKit.CARD;
import static rush.gui.UiKit.CORAL;
import static rush.gui.UiKit.CORAL_DARK;
import static rush.gui.UiKit.CREAM;
import static rush.gui.UiKit.GREEN;
import static rush.gui.UiKit.INK;
import static rush.gui.UiKit.LINE;
import static rush.gui.UiKit.MUTED;
import static rush.gui.UiKit.TEAL;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JPanel;
import javax.swing.Timer;

import rush.control.GameConfig;
import rush.control.GameController;
import rush.control.GameResult;
import rush.control.GameStats;
import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.Money;
import rush.domain.Order;

/**
 * The game window's drawing surface (boundary class). It draws the scene, the
 * side panel, the action bar and the overlays on a 1280 x 720 virtual canvas,
 * and turns clicks and key presses into choices passed to TurnFlow.
 */
final class GamePanel extends JPanel implements TurnFlow.Listener {

    private static final long serialVersionUID = 1L;

    enum Screen { TITLE, PLAYING, OVER }

    /** A clickable area drawn on the canvas. */
    private static final class UiButton {
        enum Style { OPTION, PRIMARY, PILL }

        final Rectangle2D bounds;
        final String key;
        final String label;
        final String detail;
        final Style style;
        final boolean recommended;
        final Runnable action;

        UiButton(double x, double y, double w, double h, String key, String label, String detail,
                 Style style, boolean recommended, Runnable action) {
            this.bounds = new Rectangle2D.Double(x, y, w, h);
            this.key = key;
            this.label = label;
            this.detail = detail;
            this.style = style;
            this.recommended = recommended;
            this.action = action;
        }
    }

    private static final class LogLine {
        final String text;
        final TurnFlow.LogKind kind;

        LogLine(String text, TurnFlow.LogKind kind) {
            this.text = text;
            this.kind = kind;
        }
    }

    /** One turn's log: a heading and its lines in the order they happened. */
    private static final class LogBlock {
        final String title;
        final List<LogLine> lines = new ArrayList<>();

        LogBlock(String title) {
            this.title = title;
        }
    }

    private final transient World world = new World();
    private final transient TurnFlow flow;
    private final transient GameController idleGame;
    private Screen screen = Screen.TITLE;
    private boolean showHelp;
    private boolean tipsOn = true;
    private double time;
    private final transient List<UiButton> buttons = new ArrayList<>();
    private double mouseX = -1;
    private double mouseY = -1;
    private final transient Deque<LogBlock> log = new ArrayDeque<>();
    private String toast;
    private double toastTimer;
    private double scale = 1;
    private double offsetX;
    private double offsetY;
    private long lastNanos;
    private transient ScriptedPlayer replay;
    private String replayName;
    private double replaySpeed = 1;
    private double replayWait;

    GamePanel(Supplier<GameController> factory) {
        this.flow = new TurnFlow(factory, this);
        this.idleGame = factory.get();
        setPreferredSize(new Dimension(Layout.W, Layout.H));
        setBackground(Color.BLACK);
        setFocusable(true);
        installInput();
        Timer timer = new Timer(16, e -> tick());
        timer.start();
    }

    // --------------------------------------------------------------- control

    void startGame() {
        log.clear();
        world.reset();
        screen = Screen.PLAYING;
        showHelp = false;
        flow.newGame();
        SoundFx.play(SoundFx.Sound.CLICK);
    }

    /** Plays a scripted run (same file format as the command-line test scripts). */
    void startReplay(ScriptedPlayer player, String name, double speed) {
        this.replay = player;
        this.replayName = name;
        this.replaySpeed = Math.max(0.25, speed);
        this.replayWait = 1.0;
        startGame();
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.016 : Math.min(0.05, (now - lastNanos) / 1e9);
        lastNanos = now;
        double speed = replay != null ? replaySpeed : 1;
        time += dt;
        world.sync(current());
        world.update(dt * speed);
        if (toastTimer > 0) {
            toastTimer -= dt;
        }
        if (replay != null && screen == Screen.PLAYING) {
            replayWait -= dt * speed;
            if (replayWait <= 0) {
                stepReplay();
            }
        }
        repaint();
    }

    private void stepReplay() {
        if (flow.phase() == TurnFlow.Phase.TURN_END) {
            flow.nextTurn();
            replayWait = 1.6;
        } else if (flow.phase() == TurnFlow.Phase.GAME_OVER) {
            replay = null;
        } else if (!replay.step(flow)) {
            replay = null;
            showToast("The replay script has ended - you can keep playing from here.", false);
        } else {
            replayWait = 1.1;
        }
    }

    private GameController current() {
        return flow.game() != null && screen != Screen.TITLE ? flow.game() : idleGame;
    }

    private void choose(TurnFlow.Option option) {
        SoundFx.play(SoundFx.Sound.CLICK);
        flow.choose(option);
    }

    private void nextTurn() {
        SoundFx.play(SoundFx.Sound.CLICK);
        flow.nextTurn();
    }

    // ------------------------------------------------------ TurnFlow events

    @Override
    public void onLog(String text, TurnFlow.LogKind kind) {
        if (kind == TurnFlow.LogKind.TURN || log.isEmpty()) {
            log.addFirst(new LogBlock(kind == TurnFlow.LogKind.TURN ? text : ""));
            while (log.size() > 8) {
                log.removeLast();
            }
            if (kind == TurnFlow.LogKind.TURN) {
                return;
            }
        }
        log.peekFirst().lines.add(new LogLine(text, kind));
    }

    @Override
    public void onStaffAction(TurnFlow.Option option, ActionResult result) {
        world.sync(flow.game());
        world.onStaffAction(option, result);
        if (!result.isSuccess()) {
            showToast("Not allowed: " + result.getMessage() + " Choose again.", true);
        }
    }

    @Override
    public void onTurnEnded() {
        // the action bar switches to the end-of-turn summary
    }

    @Override
    public void onGameOver() {
        screen = Screen.OVER;
        boolean won = flow.game().getResult() == GameResult.VICTORY;
        SoundFx.play(won ? SoundFx.Sound.VICTORY : SoundFx.Sound.DEFEAT);
        if (won) {
            world.effects.confetti(Layout.SCENE_W);
        }
    }

    private boolean toastIsError;

    private void showToast(String text, boolean error) {
        toast = text;
        toastIsError = error;
        toastTimer = 4.5;
    }

    // ----------------------------------------------------------------- input

    private void installInput() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = (e.getX() - offsetX) / scale;
                mouseY = (e.getY() - offsetY) / scale;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                double x = (e.getX() - offsetX) / scale;
                double y = (e.getY() - offsetY) / scale;
                for (UiButton b : new ArrayList<>(buttons)) {
                    if (b.bounds.contains(x, y)) {
                        b.action.run();
                        return;
                    }
                }
                if (showHelp) {
                    showHelp = false;
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKey(e);
            }
        });
    }

    private void onKey(KeyEvent e) {
        int code = e.getKeyCode();
        char ch = Character.toUpperCase(e.getKeyChar());
        if (code == KeyEvent.VK_ESCAPE) {
            if (showHelp) {
                showHelp = false;
            } else if (screen != Screen.PLAYING) {
                System.exit(0);
            }
            return;
        }
        if (ch == 'H') {
            showHelp = !showHelp;
            return;
        }
        if (ch == 'S') {
            SoundFx.setEnabled(!SoundFx.isEnabled());
            return;
        }
        if (ch == 'M') {
            Music.toggle();
            return;
        }
        if (ch == 'T') {
            tipsOn = !tipsOn;
            return;
        }
        for (UiButton b : new ArrayList<>(buttons)) {
            if (b.key == null) {
                continue;
            }
            boolean enter = (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) && b.key.equals("Enter");
            if (enter || b.key.equalsIgnoreCase(String.valueOf(e.getKeyChar()))) {
                b.action.run();
                return;
            }
        }
    }

    // ---------------------------------------------------------------- paint

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        scale = Math.min(getWidth() / (double) Layout.W, getHeight() / (double) Layout.H);
        offsetX = (getWidth() - Layout.W * scale) / 2;
        offsetY = (getHeight() - Layout.H * scale) / 2;
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        renderFrame(g);
        g.dispose();
    }

    /** Draws one complete frame onto a 1280 x 720 canvas. */
    void renderFrame(Graphics2D g) {
        buttons.clear();
        GameController game = current();
        double evening = SceneRenderer.evening(Math.max(1, game.getCurrentTurn()), game.getConfig().getTotalTurns());
        Shape clip = g.getClip();
        g.clipRect(0, 0, Layout.SCENE_W, Layout.SCENE_H);
        world.paint(g, game, evening);
        paintToast(g);
        g.setClip(clip);
        paintSidePanel(g, game);
        paintActionBar(g, game);
        if (screen == Screen.PLAYING && !showHelp) {
            paintHoverCard(g);
        }
        if (screen == Screen.TITLE) {
            buttons.clear();
            paintTitle(g);
        } else if (screen == Screen.OVER) {
            buttons.clear();
            paintGameOver(g, game);
        }
        if (showHelp) {
            buttons.clear();
            paintHelp(g);
        }
        paintButtons(g);
    }

    // ------------------------------------------------------------ side panel

    private void paintSidePanel(Graphics2D g, GameController game) {
        int x = Layout.SIDE_X;
        g.setPaint(new GradientPaint(x, 0, new Color(255, 246, 230), x, Layout.H, new Color(250, 228, 204)));
        g.fillRect(x, 0, Layout.W - x, Layout.H);
        g.setColor(CORAL);
        g.fillRect(x, 0, 5, Layout.H);
        int left = x + 20;
        int width = Layout.W - left - 16;

        // header: turn counter
        UiKit.card(g, left, 12, width, 78, CARD, LINE);
        GameConfig config = game.getConfig();
        int turn = game.getCurrentTurn();
        g.setFont(UiKit.title(22));
        g.setColor(CORAL);
        g.drawString("Tonight", left + 16, 42);
        g.setFont(UiKit.font(Font.BOLD, 20));
        g.setColor(INK);
        String turnText = turn == 0 ? "Doors open soon" : "Turn " + turn + " / " + config.getTotalTurns();
        FontMetrics fm = g.getFontMetrics();
        g.drawString(turnText, left + width - 16 - fm.stringWidth(turnText), 42);
        double pipW = (width - 32) / (double) config.getTotalTurns();
        for (int i = 0; i < config.getTotalTurns(); i++) {
            double px = left + 16 + i * pipW;
            g.setColor(i < turn ? (i == turn - 1 ? CORAL : new Color(255, 178, 150)) : new Color(242, 228, 214));
            g.fill(new RoundRectangle2D.Double(px, 58, pipW - 3, 16, 6, 6));
        }

        // targets
        GameStats stats = game.getStats();
        UiKit.card(g, left, 102, width, 186, CARD, LINE);
        g.setFont(UiKit.font(Font.BOLD, 13));
        g.setColor(MUTED);
        g.drawString("TONIGHT'S TARGETS", left + 16, 126);
        String avg = stats.hasPaidCustomers() ? String.format("%.1f", stats.getAverageSatisfaction()) : "N/A";
        paintGoal(g, left + 16, 140, width - 32, 0, "Paid customers",
                stats.getPaidCustomers() + " / " + config.getTargetPaidCustomers(),
                stats.getPaidCustomers() / (double) config.getTargetPaidCustomers(), 1.0, stats.meetsPaidTarget(config));
        paintGoal(g, left + 16, 186, width - 32, 1, "Revenue",
                Money.format(stats.getRevenueCents()) + " / " + Money.format(config.getTargetRevenueCents()),
                stats.getRevenueCents() / (double) config.getTargetRevenueCents(), 1.0, stats.meetsRevenueTarget(config));
        paintGoal(g, left + 16, 232, width - 32, 2, "Avg satisfaction",
                avg + "  (need " + config.getTargetAverageSatisfaction() + ")",
                stats.getAverageSatisfaction() / 100.0, config.getTargetAverageSatisfaction() / 100.0,
                stats.meetsSatisfactionTarget(config));

        // log
        UiKit.card(g, left, 300, width, 372, CARD, LINE);
        paintLog(g, left + 16, 324, width - 32, 664);
        g.setFont(UiKit.font(Font.PLAIN, 11));
        g.setColor(MUTED);
        String counts = "Served " + stats.getServedCount() + "  |  Walked out " + stats.getUnhappyDepartures();
        g.drawString(counts, left + width - 16 - g.getFontMetrics().stringWidth(counts), 324);

        double bx = left;
        bx += pill(g, bx, 684, SoundFx.isEnabled() ? "Sound on" : "Sound off", "S",
                () -> SoundFx.setEnabled(!SoundFx.isEnabled())) + 6;
        bx += pill(g, bx, 684, Music.isOn() ? "Music on" : "Music off", "M", Music::toggle) + 6;
        bx += pill(g, bx, 684, tipsOn ? "Tips on" : "Tips off", "T", () -> tipsOn = !tipsOn) + 6;
        pill(g, bx, 684, "Help", "H", () -> showHelp = !showHelp);
    }

    private void paintGoal(Graphics2D g, int x, int y, int w, int icon, String label, String value,
                           double fraction, double target, boolean met) {
        if (icon == 0) {
            UiKit.star(g, x + 9, y + 9, 10, UiKit.GOLD);
        } else if (icon == 1) {
            UiKit.coin(g, x + 9, y + 9, 9);
        } else {
            UiKit.heart(g, x + 9, y + 10, 9, CORAL);
        }
        g.setFont(UiKit.font(Font.BOLD, 13));
        g.setColor(INK);
        g.drawString(label, x + 26, y + 14);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(met ? GREEN : INK);
        g.drawString(value, x + w - fm.stringWidth(value), y + 14);
        g.setColor(new Color(244, 232, 220));
        g.fill(new RoundRectangle2D.Double(x, y + 22, w, 12, 8, 8));
        Color fill = met ? GREEN : new Color(255, 160, 90);
        g.setPaint(new GradientPaint(x, y + 22, fill.brighter(), x, y + 34, fill));
        g.fill(new RoundRectangle2D.Double(x, y + 22, w * Math.max(0, Math.min(1, fraction)), 12, 8, 8));
        if (target < 1.0) {
            g.setColor(INK);
            g.fill(new Rectangle2D.Double(x + w * target - 1, y + 19, 2.5, 18));
        }
        if (met) {
            UiKit.star(g, x + w - 4, y + 28, 7, Color.WHITE);
        }
    }

    private void paintLog(Graphics2D g, int x, int y, int w, int bottom) {
        g.setFont(UiKit.font(Font.BOLD, 13));
        g.setColor(MUTED);
        g.drawString("WHAT JUST HAPPENED", x, y);
        double ly = y + 24;
        Font body = UiKit.font(Font.PLAIN, 12);
        int blockIndex = 0;
        outer:
        for (LogBlock block : log) {
            double fade = blockIndex == 0 ? 1 : 0.6;
            if (!block.title.isEmpty()) {
                if (ly + 15 > bottom) {
                    break;
                }
                g.setColor(UiKit.alpha(blockIndex == 0 ? CORAL : new Color(240, 170, 150), 1));
                g.fill(new RoundRectangle2D.Double(x, ly - 13, g.getFontMetrics(UiKit.font(Font.BOLD, 11))
                        .stringWidth(block.title.toUpperCase()) + 14, 18, 9, 9));
                g.setFont(UiKit.font(Font.BOLD, 11));
                g.setColor(Color.WHITE);
                g.drawString(block.title.toUpperCase(), x + 7, (float) ly);
                ly += 20;
            }
            g.setFont(body);
            for (LogLine line : block.lines) {
                List<String> wrapped = UiKit.wrap(g, line.text, w - 14);
                if (ly + wrapped.size() * 15 > bottom) {
                    break outer;
                }
                Color c = colorFor(line.kind);
                g.setColor(UiKit.alpha(c, fade));
                g.fill(new java.awt.geom.Ellipse2D.Double(x + 1, ly - 8, 5, 5));
                for (String part : wrapped) {
                    g.drawString(part, x + 12, (float) ly);
                    ly += 15;
                }
                ly += 3;
            }
            ly += 8;
            blockIndex++;
        }
        if (log.isEmpty()) {
            g.setFont(body);
            g.setColor(MUTED);
            g.drawString("Events will appear here as the evening goes on.", x, (float) ly);
        }
    }

    private static Color colorFor(TurnFlow.LogKind kind) {
        switch (kind) {
            case ARRIVAL:
                return UiKit.BLUE;
            case REFUSED:
                return UiKit.RED;
            case EVENT:
                return new Color(140, 96, 70);
            case ACTION:
            default:
                return INK;
        }
    }

    private double pill(Graphics2D g, double x, double y, String text, String key, Runnable action) {
        g.setFont(UiKit.font(Font.BOLD, 11));
        double w = g.getFontMetrics().stringWidth(text + " (" + key + ")") + 18;
        buttons.add(new UiButton(x, y, w, 26, null, text + " (" + key + ")", null, UiButton.Style.PILL, false, action));
        return w;
    }

    // ------------------------------------------------------------ action bar

    private void paintActionBar(Graphics2D g, GameController game) {
        int y = Layout.BAR_Y;
        g.setPaint(new GradientPaint(0, y, new Color(255, 244, 226), 0, Layout.H, new Color(246, 224, 196)));
        g.fillRect(0, y, Layout.SCENE_W, Layout.H - y);
        g.setColor(CORAL);
        g.fillRect(0, y, Layout.SCENE_W, 5);
        if (screen != Screen.PLAYING) {
            return;
        }
        TurnFlow.Phase phase = flow.phase();
        if (phase == TurnFlow.Phase.GAME_OVER) {
            return;
        }
        if (phase == TurnFlow.Phase.TURN_END) {
            paintTurnSummary(g, game);
            return;
        }
        paintPortrait(g, phase);
        List<TurnFlow.Option> options = flow.options();
        Advisor.Tip tip = Advisor.advise(flow, options);
        if (replay != null) {
            paintTipStrip(g, "REPLAY of " + replayName + " (x" + trim(replaySpeed) + " speed): the scripted manager "
                    + "is choosing. Watch the restaurant and the log.", true);
            paintReplayChoices(g, options);
            return;
        }
        paintTipStrip(g, tipsOn ? tip.text : "Pick one task for the " + roleName(phase) + ". (Press T for tips.)", false);
        int count = Math.min(options.size(), 8);
        int cols = count > 6 ? 4 : 3;
        double gap = 8;
        double bw = (Layout.SCENE_W - 162 - 12 - gap * (cols - 1)) / cols;
        double bh = 44;
        int waitSlot = (count > 6 ? 8 : 6) - 1;
        for (int i = 0; i < count; i++) {
            TurnFlow.Option o = options.get(i);
            boolean isWait = o.kind == TurnFlow.Kind.WAIT;
            int slot = isWait ? waitSlot : i;
            double bx = 162 + (slot % cols) * (bw + gap);
            double by = y + 58 + (slot / cols) * (bh + 9);
            String key = isWait ? "0" : String.valueOf(i + 1);
            boolean best = tipsOn && tip.recommended == o;
            buttons.add(new UiButton(bx, by, bw, bh, key, o.label, o.detail, UiButton.Style.OPTION, best,
                    () -> choose(o)));
        }
    }

    private void paintReplayChoices(Graphics2D g, List<TurnFlow.Option> options) {
        g.setFont(UiKit.font(Font.PLAIN, 13));
        g.setColor(MUTED);
        double y = Layout.BAR_Y + 80;
        g.drawString("Choices available now:", 166, (float) y);
        g.setFont(UiKit.font(Font.BOLD, 13));
        g.setColor(INK);
        StringBuilder sb = new StringBuilder();
        for (TurnFlow.Option o : options) {
            if (sb.length() > 0) {
                sb.append("   |   ");
            }
            sb.append(o.label);
        }
        List<String> lines = UiKit.wrap(g, sb.toString(), Layout.SCENE_W - 190);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            g.drawString(lines.get(i), 166, (float) (y + 22 + i * 18));
        }
    }

    private void paintTipStrip(Graphics2D g, String text, boolean replayStyle) {
        double x = 162;
        double y = Layout.BAR_Y + 12;
        double w = Layout.SCENE_W - x - 12;
        double h = 38;
        UiKit.card(g, x, y, w, h, replayStyle ? UiKit.TEAL_LIGHT : UiKit.TIP,
                replayStyle ? TEAL : new Color(240, 200, 110));
        UiKit.bulb(g, x + 20, y + 19);
        g.setFont(UiKit.font(Font.BOLD, 11));
        g.setColor(replayStyle ? TEAL : new Color(200, 130, 30));
        g.drawString(replayStyle ? "REPLAY" : "MANAGER'S TIP", (float) (x + 38), (float) (y + 15));
        g.setFont(UiKit.font(Font.PLAIN, 12));
        g.setColor(INK);
        List<String> lines = UiKit.wrap(g, text, w - 150);
        if (lines.size() <= 1) {
            g.drawString(text, (float) (x + 140), (float) (y + 24));
        } else {
            g.drawString(lines.get(0), (float) (x + 140), (float) (y + 16));
            g.drawString(UiKit.fit(g, String.join(" ", lines.subList(1, lines.size())), w - 150),
                    (float) (x + 140), (float) (y + 31));
        }
    }

    private static String roleName(TurnFlow.Phase phase) {
        return phase == TurnFlow.Phase.CHEF ? "Chef" : "Waiter";
    }

    private void paintPortrait(Graphics2D g, TurnFlow.Phase phase) {
        int y = Layout.BAR_Y;
        UiKit.card(g, 12, y + 12, 136, 138, UiKit.TEAL_LIGHT, TEAL);
        Actor who = phase == TurnFlow.Phase.CHEF ? world.chef : world.waiter;
        CharacterPainter.Pose pose = new CharacterPainter.Pose();
        pose.time = time;
        pose.facing = 1;
        java.awt.geom.AffineTransform saved = g.getTransform();
        g.translate(80, y + 114);
        g.scale(0.82, 0.82);
        CharacterPainter.paint(g, who.look, 0, 0, pose);
        g.setTransform(saved);
        g.setColor(TEAL);
        g.fill(new RoundRectangle2D.Double(22, y + 118, 116, 24, 12, 12));
        g.setFont(UiKit.font(Font.BOLD, 12));
        g.setColor(Color.WHITE);
        SceneRenderer.drawCentered(g, roleName(phase).toUpperCase() + "'S TURN", 80, y + 135);
    }

    private void paintTurnSummary(Graphics2D g, GameController game) {
        int y = Layout.BAR_Y;
        GameStats s = game.getStats();
        UiKit.card(g, 12, y + 14, Layout.SCENE_W - 24, 130, CARD, LINE);
        g.setFont(UiKit.title(28));
        g.setColor(CORAL);
        g.drawString("End of turn " + game.getCurrentTurn(), 34, y + 54);
        String avg = s.hasPaidCustomers() ? String.format("%.1f", s.getAverageSatisfaction()) : "N/A";
        double sx = 36;
        double sy = y + 92;
        sx = stat(g, sx, sy, "Served", String.valueOf(s.getServedCount()));
        sx = stat(g, sx, sy, "Walked out", String.valueOf(s.getUnhappyDepartures()));
        sx = stat(g, sx, sy, "Revenue", Money.format(s.getRevenueCents()));
        sx = stat(g, sx, sy, "Avg satisfaction", avg);
        stat(g, sx, sy, "Turns used", game.getCurrentTurn() + "/" + game.getConfig().getTotalTurns());
        g.setFont(UiKit.font(Font.PLAIN, 12));
        g.setColor(MUTED);
        g.drawString("Eating, paying and waiting have been worked out. The log on the right shows what happened.",
                36, y + 132);
        if (replay == null) {
            buttons.add(new UiButton(650, y + 34, 220, 54, "Enter", "Next turn  >", "press Enter",
                    UiButton.Style.PRIMARY, false, this::nextTurn));
        }
    }

    private double stat(Graphics2D g, double x, double y, String label, String value) {
        g.setFont(UiKit.font(Font.BOLD, 18));
        g.setColor(INK);
        g.drawString(value, (float) x, (float) y);
        double w = g.getFontMetrics().stringWidth(value);
        g.setFont(UiKit.font(Font.PLAIN, 11));
        g.setColor(MUTED);
        g.drawString(label, (float) x, (float) (y + 16));
        w = Math.max(w, g.getFontMetrics().stringWidth(label));
        return x + w + 28;
    }

    private void paintButtons(Graphics2D g) {
        for (UiButton b : buttons) {
            boolean hover = b.bounds.contains(mouseX, mouseY);
            double x = b.bounds.getX();
            double y = b.bounds.getY();
            double w = b.bounds.getWidth();
            double h = b.bounds.getHeight();
            if (b.style == UiButton.Style.PRIMARY) {
                UiKit.candy(g, x, y, w, h, new Color(255, 140, 100), CORAL_DARK, hover);
                g.setFont(UiKit.font(Font.BOLD, 18));
                g.setColor(Color.WHITE);
                SceneRenderer.drawCentered(g, b.label, x + w / 2, y + (b.detail != null ? h / 2 + 2 : h / 2 + 6));
                if (b.detail != null) {
                    g.setFont(UiKit.font(Font.PLAIN, 11));
                    g.setColor(new Color(255, 236, 226));
                    SceneRenderer.drawCentered(g, b.detail, x + w / 2, y + h / 2 + 18);
                }
                continue;
            }
            if (b.style == UiButton.Style.PILL) {
                g.setColor(hover ? new Color(255, 236, 214) : CARD);
                g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
                g.setColor(LINE);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
                g.setFont(UiKit.font(Font.BOLD, 11));
                g.setColor(INK);
                SceneRenderer.drawCentered(g, b.label, x + w / 2, y + h / 2 + 4);
                continue;
            }
            // option card
            if (b.recommended) {
                double pulse = 0.5 + 0.5 * Math.sin(time * 5);
                g.setColor(UiKit.alpha(UiKit.GOLD, 0.35 + 0.35 * pulse));
                g.fill(new RoundRectangle2D.Double(x - 5, y - 5, w + 10, h + 10, 20, 20));
            }
            UiKit.card(g, x, y, w, h, hover ? new Color(255, 250, 232) : CARD,
                    b.recommended ? new Color(240, 160, 40) : LINE);
            if (b.recommended) {
                g.setColor(new Color(240, 160, 40));
                g.setStroke(new BasicStroke(2.5f));
                g.draw(new RoundRectangle2D.Double(x, y, w, h, 18, 18));
            }
            double tx = x + 10;
            if (b.key != null) {
                boolean isWait = b.key.equals("0");
                g.setColor(isWait ? new Color(180, 160, 140) : CORAL);
                g.fill(new java.awt.geom.Ellipse2D.Double(tx - 1, y + h / 2 - 12, 24, 24));
                g.setColor(Color.WHITE);
                g.setFont(UiKit.font(Font.BOLD, 13));
                SceneRenderer.drawCentered(g, b.key, tx + 11, y + h / 2 + 4.5);
                tx += 32;
            }
            double maxW = x + w - tx - 8;
            g.setFont(UiKit.font(Font.BOLD, 13));
            g.setColor(INK);
            g.drawString(UiKit.fit(g, b.label, maxW), (float) tx, (float) (y + (b.detail == null ? h / 2 + 5 : 19)));
            if (b.detail != null) {
                g.setFont(UiKit.font(Font.PLAIN, 11));
                g.setColor(MUTED);
                g.drawString(UiKit.fit(g, b.detail, maxW), (float) tx, (float) (y + 34));
            }
            if (b.recommended) {
                double bounce = Math.abs(Math.sin(time * 4)) * 3;
                double bx = x + w - 58;
                double by = y - 9 - bounce;
                UiKit.candy(g, bx, by, 50, 18, new Color(255, 200, 80), new Color(240, 150, 30), false);
                UiKit.star(g, bx + 11, by + 9, 6, Color.WHITE);
                g.setFont(UiKit.font(Font.BOLD, 10));
                g.setColor(Color.WHITE);
                g.drawString("TIP", (float) (bx + 20), (float) (by + 13));
            }
        }
    }

    private void paintToast(Graphics2D g) {
        if (toast == null || toastTimer <= 0) {
            return;
        }
        g.setFont(UiKit.font(Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        String text = UiKit.fit(g, toast, Layout.SCENE_W - 90);
        double w = fm.stringWidth(text) + 36;
        double x = (Layout.SCENE_W - w) / 2;
        float a = (float) Math.min(1, toastTimer * 2);
        Composite saved = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        Color fill = toastIsError ? UiKit.RED : TEAL;
        UiKit.candy(g, x, Layout.SCENE_H - 52, w, 34, fill.brighter(), fill, false);
        g.setColor(Color.WHITE);
        g.drawString(text, (float) (x + 18), (float) (Layout.SCENE_H - 30));
        g.setComposite(saved);
    }

    // ------------------------------------------------------------ hover card

    private void paintHoverCard(Graphics2D g) {
        if (mouseX < 0 || mouseX > Layout.SCENE_W || mouseY > Layout.SCENE_H) {
            return;
        }
        Customer c = world.customerAt(mouseX, mouseY);
        if (c == null) {
            return;
        }
        g.setFont(UiKit.font(Font.PLAIN, 12));
        List<String> lines = new ArrayList<>();
        lines.add(TurnFlow.name(c) + "  -  " + c.getTypeName());
        lines.add("Satisfaction " + c.getSatisfaction());
        for (String part : UiKit.wrap(g, "Behaviour: " + c.describeBehaviour(), 330)) {
            lines.add(part);
        }
        lines.add("Status: " + c.getStatus().name().replace('_', ' ').toLowerCase());
        Order o = c.getOrder();
        if (o != null) {
            String state = o.isActive() ? o.getStatus().name().replace('_', ' ').toLowerCase() + ", " + o.getProgress()
                    : o.getStatus().name().toLowerCase();
            lines.add(o.getLabel() + ": " + o.getItem().getName() + ", " + state + ", bill " + Money.format(o.getPriceCents()));
        } else if (c.getStatus() == CustomerStatus.SEATED) {
            lines.add("Waiting for the Waiter to take the order");
        }
        lines.add("Arrived on turn " + c.getArrivalTurn());
        g.setFont(UiKit.font(Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        int w = 0;
        for (String l : lines) {
            w = Math.max(w, fm.stringWidth(l));
        }
        w += 28;
        int h = lines.size() * 17 + 16;
        double x = Math.min(Layout.SCENE_W - w - 8, mouseX + 16);
        double y = Math.max(8, Math.min(Layout.SCENE_H - h - 8, mouseY - h - 10));
        UiKit.card(g, x, y, w, h, CARD, CORAL);
        for (int i = 0; i < lines.size(); i++) {
            g.setFont(UiKit.font(i == 0 ? Font.BOLD : Font.PLAIN, 12));
            g.setColor(i == 0 ? CORAL : INK);
            g.drawString(lines.get(i), (float) (x + 14), (float) (y + 22 + i * 17));
        }
    }

    // --------------------------------------------------------------- screens

    private void dim(Graphics2D g, int alpha) {
        g.setColor(new Color(40, 24, 30, alpha));
        g.fillRect(0, 0, Layout.W, Layout.H);
    }

    private RoundRectangle2D overlayCard(Graphics2D g, double w, double h) {
        RoundRectangle2D r = new RoundRectangle2D.Double((Layout.W - w) / 2, (Layout.H - h) / 2, w, h, 30, 30);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Double(r.getX() + 4, r.getY() + 8, w, h, 30, 30));
        g.setColor(CREAM);
        g.fill(r);
        g.setColor(CORAL);
        g.setStroke(new BasicStroke(4f));
        g.draw(r);
        return r;
    }

    private void paintTitle(Graphics2D g) {
        dim(g, 110);
        RoundRectangle2D r = overlayCard(g, 640, 430);
        double cx = r.getCenterX();
        double y = r.getY();
        // ribbon
        g.setPaint(new GradientPaint(0, (float) y + 22, new Color(255, 140, 100), 0, (float) y + 92, CORAL_DARK));
        g.fill(new RoundRectangle2D.Double(cx - 250, y + 22, 500, 72, 26, 26));
        g.setFont(UiKit.title(52));
        g.setColor(new Color(120, 30, 20, 120));
        SceneRenderer.drawCentered(g, "Restaurant Rush", cx + 3, y + 77);
        g.setColor(Color.WHITE);
        SceneRenderer.drawCentered(g, "Restaurant Rush", cx, y + 74);
        java.util.List<rush.domain.MenuItem> menu = idleGame.getRestaurant().getMenu().getItems();
        for (int i = 0; i < Math.min(3, menu.size()); i++) {
            DishPainter.paint(g, menu.get(i), cx - 90 + i * 90, y + 146, 1.5);
        }
        g.setFont(UiKit.font(Font.BOLD, 17));
        g.setColor(INK);
        SceneRenderer.drawCentered(g, "You are the manager tonight!", cx, y + 196);
        g.setFont(UiKit.font(Font.PLAIN, 14));
        g.setColor(MUTED);
        SceneRenderer.drawCentered(g, "18 turns, 2 tables, one Waiter, one Chef, one Cashier.", cx, y + 220);
        SceneRenderer.drawCentered(g, "Choose who gets seated, served and cooked for first.", cx, y + 240);
        buttons.add(new UiButton(cx - 140, y + 264, 280, 58, "Enter", "Open the restaurant", "press Enter",
                UiButton.Style.PRIMARY, false, this::startGame));
        buttons.add(new UiButton(cx - 140, y + 340, 135, 34, "H", "How to play (H)", null,
                UiButton.Style.PILL, false, () -> showHelp = true));
        buttons.add(new UiButton(cx + 5, y + 340, 135, 34, null, "Quit", null,
                UiButton.Style.PILL, false, () -> System.exit(0)));
        g.setFont(UiKit.font(Font.PLAIN, 11));
        g.setColor(MUTED);
        SceneRenderer.drawCentered(g, "SC2002 group project   |   Keys: 1-9 choose, 0 wait, Enter next turn, H help",
                cx, r.getMaxY() - 16);
    }

    private void paintHelp(Graphics2D g) {
        dim(g, 140);
        RoundRectangle2D r = overlayCard(g, 780, 560);
        double x = r.getX() + 40;
        double y = r.getY() + 56;
        g.setFont(UiKit.title(32));
        g.setColor(CORAL);
        g.drawString("How to play", (float) x, (float) y);
        String[] lines = {
            "#Your goal",
            "After the last turn you need at least 4 paid customers, $45.00 revenue, and an",
            "average satisfaction of 60 among the customers who paid.",
            "#Every turn",
            "1   A customer may arrive at the door and join the queue (satisfaction 100).",
            "2   The WAITER does one task: seat someone, take an order, or serve a READY dish.",
            "3   The CHEF cooks one unit of one order. Salad and Burger need 1 unit, Pasta 2.",
            "     A dish that becomes READY this turn can be served next turn at the earliest.",
            "4   A served customer eats for one turn, then the Cashier charges one customer per turn.",
            "5   Everyone not yet served loses satisfaction. At 0 they walk out without paying.",
            "#Need help deciding?",
            "The yellow MANAGER'S TIP suggests a good move and a TIP badge marks it.",
            "Hover over a customer to see their mood, order and cooking progress.",
            "A refused choice explains why and does not use up the task - just pick again.",
            "Keys: 1-9 choose  |  0 wait  |  Enter next turn  |  T tips  |  S sound  |  M music"
        };
        double ly = y + 30;
        for (String line : lines) {
            if (line.startsWith("#")) {
                ly += 8;
                g.setFont(UiKit.font(Font.BOLD, 14));
                g.setColor(TEAL);
                g.drawString(line.substring(1).toUpperCase(), (float) x, (float) ly);
                ly += 22;
            } else {
                g.setFont(UiKit.font(Font.PLAIN, 14));
                g.setColor(INK);
                g.drawString(line, (float) x, (float) ly);
                ly += 21;
            }
        }
        buttons.add(new UiButton(r.getCenterX() - 100, r.getMaxY() - 64, 200, 46, "Enter", "Got it!", null,
                UiButton.Style.PRIMARY, false, () -> showHelp = false));
    }

    private void paintGameOver(Graphics2D g, GameController game) {
        dim(g, 120);
        RoundRectangle2D r = overlayCard(g, 840, 580);
        double cx = r.getCenterX();
        double x = r.getX() + 44;
        double y = r.getY();
        boolean won = game.getResult() == GameResult.VICTORY;
        Color ribbon = won ? new Color(255, 170, 60) : new Color(170, 150, 170);
        g.setPaint(new GradientPaint(0, (float) y + 20, ribbon.brighter(), 0, (float) y + 86, ribbon.darker()));
        g.fill(new RoundRectangle2D.Double(cx - 220, y + 20, 440, 66, 26, 26));
        g.setFont(UiKit.title(50));
        g.setColor(Color.WHITE);
        SceneRenderer.drawCentered(g, won ? "Victory!" : "Defeat", cx, y + 70);
        if (won) {
            for (int i = 0; i < 3; i++) {
                UiKit.star(g, cx - 40 + i * 40, y + 110 - (i == 1 ? 8 : 0), i == 1 ? 18 : 14, UiKit.GOLD);
            }
        }
        g.setFont(UiKit.font(Font.PLAIN, 15));
        g.setColor(MUTED);
        SceneRenderer.drawCentered(g, won ? "The restaurant hit every target tonight." : "At least one target was missed.",
                cx, y + 146);
        GameStats s = game.getStats();
        GameConfig c = game.getConfig();
        String avg = s.hasPaidCustomers() ? String.format("%.1f", s.getAverageSatisfaction()) : "N/A";
        target(g, x, y + 182, s.meetsPaidTarget(c), "Paid customers " + s.getPaidCustomers()
                + "  (target " + c.getTargetPaidCustomers() + ")");
        target(g, x, y + 208, s.meetsRevenueTarget(c), "Revenue " + Money.format(s.getRevenueCents())
                + "  (target " + Money.format(c.getTargetRevenueCents()) + ")");
        target(g, x, y + 234, s.meetsSatisfactionTarget(c), "Average satisfaction " + avg
                + "  (target " + c.getTargetAverageSatisfaction() + ")");
        g.setFont(UiKit.font(Font.PLAIN, 13));
        g.setColor(MUTED);
        g.drawString("Served " + s.getServedCount() + "   |   Walked out " + s.getUnhappyDepartures()
                + "   |   Turns used " + game.getCurrentTurn(), (float) (x + 440), (float) (y + 208));

        double ty = y + 276;
        String[] head = {"Customer", "Type", "Dish", "Bill", "Satisfaction", "Result"};
        double[] col = {0, 140, 230, 440, 530, 650};
        UiKit.card(g, x - 14, ty - 22, 780, 30 + game.getRestaurant().getCustomers().size() * 24 + 10, CARD, LINE);
        g.setFont(UiKit.font(Font.BOLD, 13));
        g.setColor(TEAL);
        for (int i = 0; i < head.length; i++) {
            g.drawString(head[i], (float) (x + col[i]), (float) ty);
        }
        g.setFont(UiKit.font(Font.PLAIN, 13));
        double ry = ty + 26;
        for (Customer cu : game.getRestaurant().getCustomers()) {
            Order o = cu.getOrder();
            String result;
            Color rc;
            String sat;
            if (cu.getStatus() == CustomerStatus.PAID) {
                result = "Paid";
                rc = GREEN;
                sat = String.valueOf(cu.getPaidSatisfaction());
            } else if (cu.getStatus() == CustomerStatus.LEFT) {
                result = "Walked out";
                rc = UiKit.RED;
                sat = "0";
            } else {
                result = "Still dining";
                rc = MUTED;
                sat = String.valueOf(cu.getSatisfaction());
            }
            String[] row = {TurnFlow.name(cu), cu.getTypeName(), o == null ? "-" : o.getItem().getName(),
                o == null ? "-" : Money.format(o.getPriceCents()), sat, result};
            for (int i = 0; i < row.length; i++) {
                g.setColor(i == row.length - 1 ? rc : INK);
                g.drawString(UiKit.fit(g, row[i], 200), (float) (x + col[i]), (float) ry);
            }
            ry += 24;
        }
        buttons.add(new UiButton(cx - 210, r.getMaxY() - 72, 200, 52, "Enter", "Play again", "press Enter",
                UiButton.Style.PRIMARY, false, this::startGame));
        buttons.add(new UiButton(cx + 20, r.getMaxY() - 62, 190, 34, null, "Quit", null,
                UiButton.Style.PILL, false, () -> System.exit(0)));
    }

    private void target(Graphics2D g, double x, double y, boolean met, String text) {
        g.setColor(met ? GREEN : UiKit.RED);
        g.fill(new java.awt.geom.Ellipse2D.Double(x - 2, y - 15, 20, 20));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (met) {
            Path2D tick = new Path2D.Double();
            tick.moveTo(x + 3, y - 5);
            tick.lineTo(x + 7, y - 1);
            tick.lineTo(x + 13, y - 9);
            g.draw(tick);
        } else {
            g.draw(new Line2D.Double(x + 4, y - 10, x + 12, y - 2));
            g.draw(new Line2D.Double(x + 12, y - 10, x + 4, y - 2));
        }
        g.setFont(UiKit.font(Font.PLAIN, 15));
        g.setColor(INK);
        g.drawString(text, (float) (x + 28), (float) y);
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v);
    }
}
