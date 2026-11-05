package rush.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Colours, fonts and small drawing helpers for the bright cartoon-style interface. */
final class UiKit {

    static final Color CORAL = new Color(255, 112, 86);
    static final Color CORAL_DARK = new Color(214, 76, 60);
    static final Color TEAL = new Color(36, 160, 150);
    static final Color TEAL_LIGHT = new Color(206, 240, 234);
    static final Color INK = new Color(86, 54, 40);
    static final Color MUTED = new Color(156, 122, 100);
    static final Color CREAM = new Color(255, 248, 236);
    static final Color CARD = Color.WHITE;
    static final Color LINE = new Color(238, 212, 186);
    static final Color GREEN = new Color(70, 186, 104);
    static final Color GOLD = new Color(250, 186, 50);
    static final Color RED = new Color(226, 74, 66);
    static final Color BLUE = new Color(60, 130, 210);
    static final Color TIP = new Color(255, 248, 204);

    private UiKit() {
    }

    static Font font(int style, float size) {
        return new Font(Font.SANS_SERIF, style, Math.round(size));
    }

    static Font title(float size) {
        return new Font(Font.SERIF, Font.BOLD | Font.ITALIC, Math.round(size));
    }

    /** A white rounded card with a soft drop shadow. */
    static void card(Graphics2D g, double x, double y, double w, double h, Color fill, Color border) {
        g.setColor(new Color(150, 100, 60, 40));
        g.fill(new RoundRectangle2D.Double(x + 2, y + 4, w, h, 18, 18));
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 18, 18));
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new RoundRectangle2D.Double(x, y, w, h, 18, 18));
        }
    }

    /** A candy-style coloured button with a darker bottom edge. */
    static void candy(Graphics2D g, double x, double y, double w, double h, Color top, Color bottom, boolean hover) {
        g.setColor(bottom.darker());
        g.fill(new RoundRectangle2D.Double(x, y + 4, w, h, 16, 16));
        g.setPaint(new GradientPaint(0, (float) y, hover ? top.brighter() : top, 0, (float) (y + h), bottom));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 16, 16));
        g.setColor(new Color(255, 255, 255, 70));
        g.fill(new RoundRectangle2D.Double(x + 6, y + 3, w - 12, h * 0.38, 12, 12));
    }

    static void star(Graphics2D g, double cx, double cy, double r, Color c) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double rr = i % 2 == 0 ? r : r * 0.45;
            if (i == 0) {
                p.moveTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
            } else {
                p.lineTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
            }
        }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    static void coin(Graphics2D g, double cx, double cy, double r) {
        g.setColor(new Color(214, 150, 30));
        g.fill(new Ellipse2D.Double(cx - r, cy - r + 1.5, 2 * r, 2 * r));
        g.setColor(GOLD);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(new Color(255, 230, 140));
        g.fill(new Ellipse2D.Double(cx - r * 0.6, cy - r * 0.6, r * 1.2, r * 1.2));
        g.setColor(new Color(200, 130, 20));
        g.setFont(font(Font.BOLD, (float) (r * 1.2)));
        SceneRenderer.drawCentered(g, "$", cx, cy + r * 0.42);
    }

    static void heart(Graphics2D g, double cx, double cy, double r, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy + r * 0.9);
        p.curveTo(cx - r * 1.4, cy - r * 0.1, cx - r * 0.7, cy - r * 1.2, cx, cy - r * 0.45);
        p.curveTo(cx + r * 0.7, cy - r * 1.2, cx + r * 1.4, cy - r * 0.1, cx, cy + r * 0.9);
        g.setColor(c);
        g.fill(p);
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new Ellipse2D.Double(cx - r * 0.65, cy - r * 0.6, r * 0.4, r * 0.3));
    }

    static void bulb(Graphics2D g, double cx, double cy) {
        g.setColor(new Color(255, 220, 90));
        g.fill(new Ellipse2D.Double(cx - 9, cy - 11, 18, 18));
        g.setColor(new Color(170, 170, 180));
        g.fill(new RoundRectangle2D.Double(cx - 5, cy + 5, 10, 7, 3, 3));
        g.setColor(new Color(255, 250, 220));
        g.fill(new Ellipse2D.Double(cx - 5, cy - 7, 5, 5));
    }

    /** Small down arrow that points at the suggested choice. */
    static void arrow(Graphics2D g, double cx, double tipY, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx - 9, tipY - 12);
        p.lineTo(cx + 9, tipY - 12);
        p.lineTo(cx, tipY);
        p.closePath();
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
        g.setColor(c);
        g.fill(p);
        g.fill(new RoundRectangle2D.Double(cx - 4, tipY - 22, 8, 12, 3, 3));
    }

    static List<String> wrap(Graphics2D g, String text, double width) {
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    static String fit(Graphics2D g, String text, double width) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String t = text;
        while (t.length() > 3 && fm.stringWidth(t + "...") > width) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.max(0, Math.min(255, 255 * a)));
    }
}
