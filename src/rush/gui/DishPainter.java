package rush.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;

import rush.domain.MenuItem;

/** Draws food. (cx, cy) is the centre of the plate; scale 1 is about 40 px wide. */
final class DishPainter {

    private DishPainter() {
    }

    static void paint(Graphics2D g, MenuItem item, double cx, double cy, double scale) {
        AffineTransform saved = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        paintDish(g, item.getName());
        g.setTransform(saved);
    }

    /** Draws one dish by name at the origin; unknown dishes get a covered plate. */
    static void paintDish(Graphics2D g, String name) {
        String n = name.toLowerCase();
        if (n.equals("salad")) {
            salad(g);
        } else if (n.equals("burger")) {
            burger(g);
        } else if (n.equals("pasta")) {
            pasta(g);
        } else {
            cloche(g);
        }
    }

    static void plate(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new Ellipse2D.Double(-21, -3, 42, 12));
        g.setColor(new Color(248, 248, 244));
        g.fill(new Ellipse2D.Double(-21, -7, 42, 13));
        g.setColor(new Color(220, 220, 214));
        g.draw(new Ellipse2D.Double(-15, -4.5, 30, 8));
    }

    private static void salad(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new Ellipse2D.Double(-17, -1, 34, 9));
        g.setColor(new Color(235, 240, 245));
        g.fill(new Arc2D.Double(-17, -16, 34, 26, 180, 180, Arc2D.CHORD));
        Color[] greens = {new Color(90, 170, 70), new Color(60, 140, 60), new Color(130, 190, 80)};
        for (int i = 0; i < 7; i++) {
            g.setColor(greens[i % 3]);
            g.fill(new Ellipse2D.Double(-14 + i * 4, -10 - (i % 2) * 4, 10, 8));
        }
        g.setColor(new Color(220, 60, 50));
        g.fill(new Ellipse2D.Double(-6, -11, 6, 6));
        g.fill(new Ellipse2D.Double(5, -9, 5, 5));
        g.setColor(new Color(250, 240, 200));
        g.fill(new Ellipse2D.Double(-1, -14, 5, 4));
    }

    private static void burger(Graphics2D g) {
        plate(g);
        g.setColor(new Color(214, 150, 70));
        g.fill(new RoundRectangle2D.Double(-13, -6, 26, 6, 4, 4));
        g.setColor(new Color(110, 60, 35));
        g.fill(new RoundRectangle2D.Double(-14, -10, 28, 6, 5, 5));
        g.setColor(new Color(250, 200, 60));
        g.fill(new RoundRectangle2D.Double(-14, -12, 28, 3, 2, 2));
        g.setColor(new Color(90, 170, 70));
        g.fill(new RoundRectangle2D.Double(-15, -14, 30, 3, 3, 3));
        g.setColor(new Color(224, 160, 74));
        g.fill(new Arc2D.Double(-13, -26, 26, 26, 0, 180, Arc2D.CHORD));
        g.setColor(new Color(250, 240, 210));
        g.fill(new Ellipse2D.Double(-6, -22, 2.5, 1.5));
        g.fill(new Ellipse2D.Double(1, -24, 2.5, 1.5));
        g.fill(new Ellipse2D.Double(5, -20, 2.5, 1.5));
    }

    private static void pasta(Graphics2D g) {
        plate(g);
        g.setColor(new Color(240, 200, 100));
        g.fill(new Ellipse2D.Double(-14, -11, 28, 12));
        g.setColor(new Color(220, 175, 80));
        g.setStroke(new BasicStroke(1.4f));
        for (int i = 0; i < 4; i++) {
            g.draw(new QuadCurve2D.Double(-12 + i * 3, -4, -2 + i * 2, -13, 10 - i * 2, -5));
        }
        g.setColor(new Color(200, 50, 40));
        g.fill(new Ellipse2D.Double(-7, -11, 14, 7));
        g.setColor(new Color(80, 150, 70));
        g.fill(new Ellipse2D.Double(-1, -12, 4, 3));
    }

    private static void cloche(Graphics2D g) {
        plate(g);
        g.setColor(new Color(200, 200, 208));
        g.fill(new Arc2D.Double(-15, -22, 30, 32, 0, 180, Arc2D.CHORD));
        g.setColor(new Color(160, 160, 170));
        g.fill(new Ellipse2D.Double(-2.5, -26, 5, 5));
    }
}
