package rush.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

import rush.domain.MenuItem;
import rush.domain.Money;

/** Draws the isometric restaurant: walls, floor, kitchen, furniture and lighting. */
final class SceneRenderer {

    static final Color WALL_RIGHT = new Color(255, 222, 170);
    static final Color WALL_LEFT = new Color(242, 196, 146);
    static final Color STRIPE = new Color(255, 236, 200);
    static final Color WOOD = new Color(196, 124, 72);
    static final Color WOOD_DARK = new Color(150, 88, 50);
    static final Color WOOD_LIGHT = new Color(226, 160, 100);
    static final Color CHALK = new Color(240, 238, 222);
    static final Color OUTLINE = new Color(90, 50, 30, 90);

    private SceneRenderer() {
    }

    static double evening(int turn, int totalTurns) {
        if (totalTurns <= 1) {
            return 0;
        }
        return Math.max(0, Math.min(1, (turn - 1) / (double) (totalTurns - 1)));
    }

    // ----- helpers -----

    static Point2D.Double iso(double gx, double gy) {
        return Layout.iso(gx, gy);
    }

    static Point2D.Double up(Point2D p, double h) {
        return new Point2D.Double(p.getX(), p.getY() - h);
    }

    static Path2D poly(Point2D... pts) {
        Path2D p = new Path2D.Double();
        p.moveTo(pts[0].getX(), pts[0].getY());
        for (int i = 1; i < pts.length; i++) {
            p.lineTo(pts[i].getX(), pts[i].getY());
        }
        p.closePath();
        return p;
    }

    /** Transform for drawing flat on a surface along +gx (right wall, front-left faces). */
    static AffineTransform alongX(Point2D origin) {
        return new AffineTransform(1, 0.5, 0, 1, origin.getX(), origin.getY());
    }

    /** Transform for drawing flat on a surface along -gy (left wall, front-right faces). */
    static AffineTransform alongY(Point2D origin) {
        return new AffineTransform(1, -0.5, 0, 1, origin.getX(), origin.getY());
    }

    /** An isometric box with a lit top and two shaded front sides. */
    static void box(Graphics2D g, double gx0, double gy0, double gx1, double gy1, double h, Color top) {
        Point2D a = iso(gx0, gy0);
        Point2D b = iso(gx1, gy0);
        Point2D c = iso(gx1, gy1);
        Point2D d = iso(gx0, gy1);
        g.setColor(shade(top, 0.78));
        g.fill(poly(d, c, up(c, h), up(d, h)));
        g.setColor(shade(top, 0.64));
        g.fill(poly(b, c, up(c, h), up(b, h)));
        g.setColor(top);
        g.fill(poly(up(a, h), up(b, h), up(c, h), up(d, h)));
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1f));
        g.draw(poly(up(a, h), up(b, h), up(c, h), up(d, h)));
        g.draw(new Line2D.Double(c, up(c, h)));
    }

    static Color shade(Color c, double f) {
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f), c.getAlpha());
    }

    static void drawCentered(Graphics2D g, String text, double cx, double baseline) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) baseline);
    }

    static Color blend(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color blend3(Color a, Color b, Color c, double t) {
        return t < 0.5 ? blend(a, b, t * 2) : blend(b, c, (t - 0.5) * 2);
    }

    // ----- the room -----

    /** Everything that only changes once a turn (World caches it as an image). */
    static void paintStaticRoom(Graphics2D g, double evening, int turn, int totalTurns, List<MenuItem> menu) {
        g.setPaint(new GradientPaint(0, 0, new Color(74, 52, 86), 0, Layout.SCENE_H, new Color(40, 30, 54)));
        g.fillRect(0, 0, Layout.SCENE_W, Layout.SCENE_H);
        paintSign(g);
        paintWalls(g);
        paintFloor(g);
        paintWindow(g, evening);
        paintMenuBoard(g, menu);
        paintPicture(g);
        paintClock(g, turn, totalTurns);
        paintKitchenWall(g);
    }

    /** Parts of the room that move every frame. */
    static void paintAnimatedRoom(Graphics2D g, double evening, double time, double doorOpen) {
        paintStars(g, evening, time);
        paintDoor(g, doorOpen, evening);
        paintStringLights(g, time);
    }

    private static void paintSign(Graphics2D g) {
        g.setColor(new Color(110, 70, 40));
        g.fillRect(70, 0, 4, 22);
        g.fillRect(226, 0, 4, 22);
        RoundRectangle2D board = new RoundRectangle2D.Double(26, 20, 248, 56, 18, 18);
        g.setPaint(new GradientPaint(0, 20, new Color(255, 120, 90), 0, 76, new Color(226, 74, 70)));
        g.fill(board);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Double(32, 26, 236, 44, 14, 14));
        g.setFont(new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 28));
        g.setColor(new Color(120, 30, 30, 120));
        drawCentered(g, "Restaurant Rush", 152, 59);
        g.setColor(Color.WHITE);
        drawCentered(g, "Restaurant Rush", 150, 57);
    }

    private static void paintWalls(Graphics2D g) {
        double h = Layout.WALL_H;
        Point2D o = iso(0, 0);
        Point2D r = iso(Layout.GX, 0);
        Point2D l = iso(0, Layout.GY);
        // right wall (dining part, then kitchen tiles)
        Point2D k = iso(Layout.KITCHEN_GX, 0);
        g.setColor(WALL_RIGHT);
        g.fill(poly(o, k, up(k, h), up(o, h)));
        g.setColor(new Color(236, 244, 248));
        g.fill(poly(k, r, up(r, h), up(k, h)));
        // left wall
        g.setColor(WALL_LEFT);
        g.fill(poly(o, l, up(l, h), up(o, h)));
        // stripes
        g.setStroke(new BasicStroke(6f));
        for (double gx = 0.25; gx < Layout.KITCHEN_GX; gx += 0.5) {
            Point2D p = iso(gx, 0);
            g.setColor(STRIPE);
            g.draw(new Line2D.Double(p.getX(), p.getY() - 48, p.getX(), p.getY() - h + 12));
        }
        for (double gy = 0.25; gy < Layout.GY; gy += 0.5) {
            Point2D p = iso(0, gy);
            g.setColor(new Color(250, 212, 170));
            g.draw(new Line2D.Double(p.getX(), p.getY() - 48, p.getX(), p.getY() - h + 12));
        }
        // kitchen wall tiles
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(200, 214, 222));
        for (double hh = 18; hh < h; hh += 18) {
            g.draw(new Line2D.Double(up(k, hh), up(r, hh)));
        }
        for (double gx = Layout.KITCHEN_GX; gx <= Layout.GX; gx += 0.5) {
            Point2D p = iso(gx, 0);
            g.draw(new Line2D.Double(p, up(p, h)));
        }
        // wainscoting on the dining walls
        g.setColor(WOOD);
        g.fill(poly(o, k, up(k, 44), up(o, 44)));
        g.setColor(shade(WOOD, 0.88));
        g.fill(poly(o, l, up(l, 44), up(o, 44)));
        g.setColor(WOOD_LIGHT);
        g.setStroke(new BasicStroke(4f));
        g.draw(new Line2D.Double(up(o, 44), up(k, 44)));
        g.draw(new Line2D.Double(up(o, 44), up(l, 44)));
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(WOOD_DARK);
        for (double gx = 0.2; gx < Layout.KITCHEN_GX - 0.6; gx += 0.9) {
            AffineTransform saved = g.getTransform();
            g.transform(alongX(iso(gx, 0)));
            g.draw(new RoundRectangle2D.Double(4, -36, 20, 28, 4, 4));
            g.setTransform(saved);
        }
        for (double gy = Layout.GY - 0.2; gy > 0.6; gy -= 0.9) {
            AffineTransform saved = g.getTransform();
            g.transform(alongY(iso(0, gy)));
            g.draw(new RoundRectangle2D.Double(4, -36, 20, 28, 4, 4));
            g.setTransform(saved);
        }
        // wall caps (thickness on top)
        g.setColor(new Color(170, 104, 64));
        g.fill(poly(up(o, h), up(r, h), up(r, h + 8), up(o, h + 8)));
        g.setColor(new Color(150, 90, 54));
        g.fill(poly(up(o, h), up(l, h), up(l, h + 8), up(o, h + 8)));
        g.setColor(new Color(110, 66, 40));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Line2D.Double(o, up(o, h)));
    }

    private static void paintFloor(Graphics2D g) {
        for (int gx = 0; gx < Layout.GX; gx++) {
            for (int gy = 0; gy < Layout.GY; gy++) {
                boolean kitchen = gx >= Layout.KITCHEN_GX && gy < 3;
                boolean odd = (gx + gy) % 2 == 0;
                Color c = kitchen ? (odd ? new Color(240, 244, 248) : new Color(186, 200, 214))
                        : (odd ? new Color(255, 242, 220) : new Color(250, 206, 186));
                g.setColor(c);
                Path2D tile = poly(iso(gx, gy), iso(gx + 1, gy), iso(gx + 1, gy + 1), iso(gx, gy + 1));
                g.fill(tile);
                g.setColor(new Color(0, 0, 0, 14));
                g.draw(tile);
            }
        }
        // floor thickness at the front, like a diorama
        Point2D l = iso(0, Layout.GY);
        Point2D f = iso(Layout.GX, Layout.GY);
        Point2D r = iso(Layout.GX, 0);
        g.setColor(new Color(176, 112, 70));
        g.fill(poly(l, f, up(f, -18), up(l, -18)));
        g.setColor(new Color(140, 86, 54));
        g.fill(poly(r, f, up(f, -18), up(r, -18)));
        // rug under the tables
        Path2D rug = poly(iso(2.7, 3.7), iso(9.6, 3.7), iso(9.6, 9.0), iso(2.7, 9.0));
        g.setColor(new Color(196, 70, 86));
        g.fill(rug);
        g.setColor(new Color(255, 212, 120));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(poly(iso(2.95, 3.95), iso(9.35, 3.95), iso(9.35, 8.75), iso(2.95, 8.75)));
        g.setStroke(new BasicStroke(1f));
        for (double gx = 3.4; gx < 9.3; gx += 0.8) {
            Point2D p = iso(gx, 4.1);
            g.fill(poly(new Point2D.Double(p.getX(), p.getY() - 3), new Point2D.Double(p.getX() + 5, p.getY()),
                    new Point2D.Double(p.getX(), p.getY() + 3), new Point2D.Double(p.getX() - 5, p.getY())));
        }
        // door mat
        g.setColor(new Color(120, 160, 110));
        g.fill(poly(iso(0.05, 6.2), iso(1.0, 6.2), iso(1.0, 7.6), iso(0.05, 7.6)));
    }

    /** Draws a rectangle on the right wall between gx0 and gx1 (local px, negative y is up). */
    private static AffineTransform onRightWall(Graphics2D g, double gx0) {
        AffineTransform saved = g.getTransform();
        g.transform(alongX(iso(gx0, 0)));
        return saved;
    }

    private static AffineTransform onLeftWall(Graphics2D g, double gyFront) {
        AffineTransform saved = g.getTransform();
        g.transform(alongY(iso(0, gyFront)));
        return saved;
    }

    private static final double WIN_X0 = 0.9;
    private static final double WIN_W = 2.5 * 32;

    private static void paintWindow(Graphics2D g, double evening) {
        AffineTransform saved = onRightWall(g, WIN_X0);
        double w = WIN_W;
        double y0 = -124;
        double h = 74;
        Color top = blend3(new Color(255, 160, 110), new Color(110, 80, 150), new Color(18, 22, 58), evening);
        Color bottom = blend3(new Color(255, 214, 140), new Color(230, 120, 120), new Color(40, 40, 90), evening);
        g.setPaint(new GradientPaint(0, (float) y0, top, 0, (float) (y0 + h), bottom));
        g.fill(new Rectangle2D.Double(0, y0, w, h));
        if (evening < 0.6) {
            g.setColor(new Color(255, 236, 160, (int) (230 * (1 - evening / 0.6))));
            g.fill(new Ellipse2D.Double(w * 0.6, y0 + 14 + evening * 50, 18, 18));
        } else {
            g.setColor(new Color(250, 246, 220));
            g.fill(new Ellipse2D.Double(w * 0.2, y0 + 8, 15, 15));
            g.setColor(top);
            g.fill(new Ellipse2D.Double(w * 0.2 + 5, y0 + 6, 15, 15));
        }
        int[] heights = {26, 40, 22, 46, 30, 38};
        double bw = w / heights.length;
        for (int i = 0; i < heights.length; i++) {
            double bx = i * bw;
            double by = y0 + h - heights[i];
            g.setColor(new Color(60, 48, 80));
            g.fill(new Rectangle2D.Double(bx, by, bw - 1.5, heights[i]));
            g.setColor(new Color(255, 214, 120, (int) (90 + evening * 160)));
            for (double wy = by + 5; wy < y0 + h - 4; wy += 8) {
                if (((int) (wy + i * 7)) % 3 != 0) {
                    g.fill(new Rectangle2D.Double(bx + 3, wy, 2.5, 3));
                    g.fill(new Rectangle2D.Double(bx + 8, wy, 2.5, 3));
                }
            }
        }
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5f));
        g.draw(new Rectangle2D.Double(0, y0, w, h));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Line2D.Double(w / 2, y0, w / 2, y0 + h));
        g.draw(new Line2D.Double(0, y0 + h / 2, w, y0 + h / 2));
        // sill, curtains and flower box
        g.setColor(WOOD_LIGHT);
        g.fill(new Rectangle2D.Double(-8, y0 + h + 2, w + 16, 6));
        g.setPaint(new GradientPaint(-14, 0, new Color(240, 90, 110), 6, 0, new Color(200, 60, 86)));
        g.fill(new RoundRectangle2D.Double(-14, y0 - 8, 16, h + 22, 8, 8));
        g.setPaint(new GradientPaint((float) w - 2, 0, new Color(240, 90, 110), (float) w + 14, 0, new Color(200, 60, 86)));
        g.fill(new RoundRectangle2D.Double(w - 2, y0 - 8, 16, h + 22, 8, 8));
        g.setColor(new Color(230, 180, 60));
        g.fill(new Rectangle2D.Double(-18, y0 - 12, w + 36, 5));
        g.setTransform(saved);
    }

    private static void paintStars(Graphics2D g, double evening, double time) {
        if (evening <= 0.45) {
            return;
        }
        AffineTransform saved = onRightWall(g, WIN_X0);
        int alpha = (int) Math.min(255, (evening - 0.45) / 0.55 * 255);
        for (int i = 0; i < 9; i++) {
            double sx = 6 + (i * 37 % (WIN_W - 12));
            double sy = -120 + (i * 13 % 30);
            double tw = 0.6 + 0.4 * Math.sin(time * 2 + i);
            g.setColor(new Color(255, 255, 230, (int) (alpha * tw)));
            g.fill(new Ellipse2D.Double(sx, sy, 2.2, 2.2));
        }
        g.setTransform(saved);
    }

    private static void paintMenuBoard(Graphics2D g, List<MenuItem> menu) {
        AffineTransform saved = onRightWall(g, 3.85);
        double w = 2.75 * 32;
        double y0 = -132;
        double h = 88;
        g.setColor(WOOD_DARK);
        g.fill(new RoundRectangle2D.Double(-5, y0 - 5, w + 10, h + 10, 8, 8));
        g.setColor(new Color(52, 72, 62));
        g.fill(new Rectangle2D.Double(0, y0, w, h));
        g.setColor(CHALK);
        g.setFont(new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 12));
        drawCentered(g, "Menu", w / 2, y0 + 13);
        int n = menu.size();
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, n > 3 ? 7 : 8));
        FontMetrics fm = g.getFontMetrics();
        double lineH = n > 3 ? 12.5 : 17;
        for (int i = 0; i < n; i++) {
            MenuItem item = menu.get(i);
            double ly = y0 + 28 + i * lineH;
            String price = Money.format(item.getPriceCents());
            String name = item.getName().replace(" Combo", "");
            while (fm.stringWidth(name) > w - 10 - fm.stringWidth(price) && name.length() > 4) {
                name = name.substring(0, name.length() - 2) + ".";
            }
            g.drawString(name, 4, (float) ly);
            g.drawString(price, (float) (w - 4 - fm.stringWidth(price)), (float) ly);
        }
        g.setTransform(saved);
    }

    private static void paintPicture(Graphics2D g) {
        AffineTransform saved = onLeftWall(g, 4.9);
        g.setColor(new Color(230, 180, 70));
        g.fill(new Rectangle2D.Double(-4, -124, 52, 46));
        g.setPaint(new GradientPaint(0, -120, new Color(255, 190, 130), 0, -82, new Color(110, 160, 210)));
        g.fill(new Rectangle2D.Double(0, -120, 44, 38));
        g.setColor(new Color(255, 230, 150));
        g.fill(new Ellipse2D.Double(24, -112, 10, 10));
        g.setColor(new Color(70, 130, 90));
        Path2D hill = new Path2D.Double();
        hill.moveTo(0, -82);
        hill.quadTo(16, -102, 30, -86);
        hill.quadTo(38, -94, 44, -88);
        hill.lineTo(44, -82);
        hill.closePath();
        g.fill(hill);
        g.setTransform(saved);
    }

    /** A wall clock whose hands move forward each turn (6 pm to midnight). */
    private static void paintClock(Graphics2D g, int turn, int totalTurns) {
        AffineTransform saved = onLeftWall(g, 2.6);
        double cx = 16;
        double cy = -104;
        g.setColor(WOOD_DARK);
        g.fill(new Ellipse2D.Double(cx - 17, cy - 17, 34, 34));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(cx - 14, cy - 14, 28, 28));
        g.setColor(new Color(80, 60, 50));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            g.fill(new Ellipse2D.Double(cx + Math.sin(a) * 11 - 1, cy - Math.cos(a) * 11 - 1, 2, 2));
        }
        double minutes = 18 * 60 + Math.max(0, turn - 1) * (360.0 / Math.max(1, totalTurns - 1));
        double hourAngle = (minutes / 60.0 % 12) / 12 * 2 * Math.PI;
        double minAngle = (minutes % 60) / 60 * 2 * Math.PI;
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, cy, cx + Math.sin(hourAngle) * 7, cy - Math.cos(hourAngle) * 7));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(200, 60, 60));
        g.draw(new Line2D.Double(cx, cy, cx + Math.sin(minAngle) * 11, cy - Math.cos(minAngle) * 11));
        g.setTransform(saved);
    }

    private static void paintKitchenWall(Graphics2D g) {
        // shelf with jars
        AffineTransform saved = onRightWall(g, 7.2);
        g.setColor(WOOD);
        g.fill(new Rectangle2D.Double(0, -104, 46, 5));
        Color[] jars = {new Color(240, 130, 70), new Color(130, 190, 100), new Color(250, 210, 90)};
        for (int i = 0; i < 3; i++) {
            g.setColor(jars[i]);
            g.fill(new RoundRectangle2D.Double(3 + i * 15, -120, 11, 16, 4, 4));
            g.setColor(new Color(120, 120, 130));
            g.fill(new Rectangle2D.Double(4 + i * 15, -123, 9, 4));
        }
        g.setTransform(saved);
        // extractor hood over the stoves
        saved = onRightWall(g, 8.3);
        double w = 2.4 * 32;
        g.setPaint(new GradientPaint(0, -136, new Color(214, 220, 228), 0, -88, new Color(170, 178, 190)));
        Path2D hood = new Path2D.Double();
        hood.moveTo(18, -136);
        hood.lineTo(w - 18, -136);
        hood.lineTo(w + 4, -92);
        hood.lineTo(-4, -92);
        hood.closePath();
        g.fill(hood);
        g.setColor(new Color(140, 148, 160));
        g.fill(new Rectangle2D.Double(-6, -94, w + 12, 5));
        g.setTransform(saved);
        // a small sign
        saved = onRightWall(g, 10.95);
        g.setColor(new Color(255, 120, 90));
        g.fill(new RoundRectangle2D.Double(0, -126, 32, 16, 6, 6));
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
        drawCentered(g, "CHEF", 16, -115);
        g.setTransform(saved);
    }

    private static void paintDoor(Graphics2D g, double open, double evening) {
        AffineTransform saved = onLeftWall(g, 7.75);
        double w = 1.7 * 32;
        double h = 104;
        g.setColor(WOOD_DARK);
        g.fill(new Rectangle2D.Double(-6, -h - 6, w + 12, h + 6));
        Color outside = blend3(new Color(255, 190, 130), new Color(140, 100, 150), new Color(30, 30, 64), evening);
        g.setColor(outside);
        g.fill(new Rectangle2D.Double(0, -h, w, h));
        double panelW = w * (1 - 0.8 * open);
        g.setPaint(new GradientPaint(0, 0, new Color(214, 120, 76), (float) w, 0, new Color(180, 96, 58)));
        g.fill(new Rectangle2D.Double(0, -h, panelW, h));
        if (open < 0.4) {
            g.setColor(new Color(200, 236, 250, 200));
            g.fill(new RoundRectangle2D.Double(9, -h + 10, w - 18, 34, 10, 10));
            g.setColor(new Color(40, 40, 40));
            g.fill(new RoundRectangle2D.Double(10, -h + 48, w - 20, 14, 5, 5));
            g.setColor(new Color(120, 255, 160));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            drawCentered(g, "OPEN", w / 2, -h + 59);
            g.setColor(new Color(250, 210, 90));
            g.fill(new Ellipse2D.Double(w - 12, -52, 6, 6));
        }
        g.setColor(new Color(240, 200, 80));
        g.fill(new Arc2D.Double(w / 2 - 6, -h - 20, 12, 13, 0, 180, Arc2D.CHORD));
        g.setTransform(saved);
    }

    private static void paintStringLights(Graphics2D g, double time) {
        Point2D o = up(iso(0, 0), Layout.WALL_H - 6);
        Point2D r = up(iso(Layout.KITCHEN_GX, 0), Layout.WALL_H - 6);
        Point2D l = up(iso(0, Layout.GY), Layout.WALL_H - 6);
        Color[] bulbs = {new Color(255, 120, 120), new Color(255, 220, 110), new Color(140, 210, 255), new Color(150, 240, 150)};
        int i = 0;
        for (Point2D[] seg : new Point2D[][] {{o, r}, {o, l}}) {
            QuadCurve2D wire = new QuadCurve2D.Double(seg[0].getX(), seg[0].getY(),
                    (seg[0].getX() + seg[1].getX()) / 2, (seg[0].getY() + seg[1].getY()) / 2 + 22,
                    seg[1].getX(), seg[1].getY());
            g.setColor(new Color(70, 50, 40));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(wire);
            for (double t = 0.07; t < 0.97; t += 0.095) {
                double u = 1 - t;
                double x = u * u * wire.getX1() + 2 * u * t * wire.getCtrlX() + t * t * wire.getX2();
                double y = u * u * wire.getY1() + 2 * u * t * wire.getCtrlY() + t * t * wire.getY2();
                Color c = bulbs[i % bulbs.length];
                double tw = 0.7 + 0.3 * Math.sin(time * 2.5 + i * 1.7);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (70 * tw)));
                g.fill(new Ellipse2D.Double(x - 7, y - 3, 14, 14));
                g.setColor(c);
                g.fill(new Ellipse2D.Double(x - 3, y + 1, 6, 8));
                i++;
            }
        }
    }

    // ----- furniture -----

    /** Cashier counter with till, drawn as two one-tile pieces so the depth looks right. */
    static void paintCashierSegment(Graphics2D g, int segment) {
        double gy0 = segment == 0 ? 1.0 : 2.1;
        double gy1 = segment == 0 ? 2.1 : 3.2;
        box(g, 1.0, gy0, 1.8, gy1, 32, new Color(236, 170, 110));
        if (segment == 0) {
            Point2D p = up(iso(1.4, 1.55), 32);
            g.setColor(new Color(90, 96, 110));
            g.fill(new RoundRectangle2D.Double(p.getX() - 13, p.getY() - 20, 26, 18, 5, 5));
            g.setColor(new Color(140, 230, 170));
            g.fill(new Rectangle2D.Double(p.getX() - 9, p.getY() - 17, 18, 6));
            g.setColor(new Color(230, 230, 236));
            g.fill(new Rectangle2D.Double(p.getX() - 9, p.getY() - 8, 18, 4));
        } else {
            AffineTransform saved = g.getTransform();
            g.transform(alongY(iso(1.8, 3.2)));
            g.setColor(new Color(255, 120, 90));
            g.fill(new RoundRectangle2D.Double(4, -26, 26, 14, 5, 5));
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            drawCentered(g, "PAY", 17, -16);
            g.setTransform(saved);
        }
    }

    /** The Host's podium by the entrance, with a reservation book and a little lamp. */
    static void paintPodium(Graphics2D g) {
        double gx = Layout.PODIUM[0];
        double gy = Layout.PODIUM[1];
        box(g, gx - 0.28, gy - 0.28, gx + 0.28, gy + 0.28, 42, new Color(150, 90, 54));
        Point2D top = up(iso(gx, gy), 42);
        g.setColor(new Color(250, 246, 236));
        g.fill(new RoundRectangle2D.Double(top.getX() - 10, top.getY() - 6, 20, 9, 2, 2));
        g.setColor(new Color(190, 50, 60));
        g.fill(new Rectangle2D.Double(top.getX() - 1, top.getY() - 6, 2, 9));
        AffineTransform saved = g.getTransform();
        g.transform(alongX(iso(gx - 0.28, gy + 0.28)));
        g.setColor(new Color(255, 200, 80));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
        g.drawString("HOST", 2, -24);
        g.setTransform(saved);
    }

    /** One tile of the kitchen back row: 0-1 stoves, 2 prep counter, 3 sink, 4 fridge. */
    static void paintKitchenSegment(Graphics2D g, int i, double time, boolean cooking) {
        double gx0 = Layout.KITCHEN_GX + i;
        double gx1 = gx0 + 1;
        if (i == 4) {
            box(g, gx0 + 0.05, 0.05, gx1 - 0.05, 0.95, 96, new Color(236, 242, 248));
            AffineTransform saved = g.getTransform();
            g.transform(alongX(iso(gx0 + 0.05, 0.95)));
            g.setColor(new Color(170, 180, 192));
            g.fill(new Rectangle2D.Double(2, -60, 26, 1.5));
            g.fill(new Rectangle2D.Double(22, -88, 3, 20));
            g.fill(new Rectangle2D.Double(22, -50, 3, 20));
            g.setColor(new Color(120, 200, 240));
            g.fill(new Ellipse2D.Double(5, -84, 6, 6));
            g.setTransform(saved);
            return;
        }
        Color top = i <= 1 ? new Color(206, 212, 222) : new Color(236, 236, 240);
        box(g, gx0, 0, gx1, 0.95, 38, top);
        Point2D c = up(iso(gx0 + 0.5, 0.48), 38);
        AffineTransform saved = g.getTransform();
        g.transform(alongX(iso(gx0, 0.95)));
        if (i <= 1) {
            g.setColor(new Color(60, 62, 70));
            g.fill(new RoundRectangle2D.Double(4, -30, 24, 20, 4, 4));
            int glow = cooking ? 150 + (int) (60 * Math.sin(time * 6)) : 60;
            g.setColor(new Color(255, 140, 50, Math.max(0, Math.min(255, glow))));
            g.fill(new RoundRectangle2D.Double(7, -27, 18, 13, 3, 3));
            g.setColor(new Color(110, 110, 120));
            g.fill(new Rectangle2D.Double(4, -35, 24, 3));
        } else if (i == 2) {
            g.setColor(new Color(150, 110, 80));
            g.fill(new Rectangle2D.Double(4, -32, 24, 26));
            g.setColor(new Color(120, 86, 60));
            g.fill(new Rectangle2D.Double(14, -26, 4, 3));
        } else {
            g.setColor(new Color(200, 206, 216));
            g.fill(new Rectangle2D.Double(4, -32, 11, 26));
            g.fill(new Rectangle2D.Double(17, -32, 11, 26));
        }
        g.setTransform(saved);
        if (i <= 1) {
            g.setColor(new Color(50, 50, 56));
            g.fill(new Ellipse2D.Double(c.getX() - 22, c.getY() - 6, 16, 8));
            g.fill(new Ellipse2D.Double(c.getX() + 4, c.getY() - 2, 16, 8));
            if (i == 0) {
                // stock pot
                g.setColor(new Color(170, 176, 190));
                g.fill(new RoundRectangle2D.Double(c.getX() - 24, c.getY() - 26, 20, 22, 5, 5));
                g.setColor(new Color(130, 136, 150));
                g.fill(new Ellipse2D.Double(c.getX() - 24, c.getY() - 30, 20, 8));
            } else {
                // frying pan
                g.setColor(new Color(40, 40, 46));
                g.fill(new Ellipse2D.Double(c.getX() - 2, c.getY() - 6, 22, 10));
                g.fill(new Rectangle2D.Double(c.getX() + 18, c.getY() - 3, 14, 3));
            }
        } else if (i == 2) {
            g.setColor(new Color(214, 160, 100));
            g.fill(new RoundRectangle2D.Double(c.getX() - 14, c.getY() - 6, 26, 12, 4, 4));
            g.setColor(new Color(230, 70, 60));
            g.fill(new Ellipse2D.Double(c.getX() - 8, c.getY() - 4, 7, 6));
            g.setColor(new Color(110, 190, 80));
            g.fill(new Ellipse2D.Double(c.getX() + 1, c.getY() - 5, 9, 6));
        } else {
            g.setColor(new Color(160, 200, 230));
            g.fill(new Ellipse2D.Double(c.getX() - 15, c.getY() - 6, 30, 13));
            g.setColor(new Color(150, 156, 170));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Arc2D.Double(c.getX() - 4, c.getY() - 20, 12, 20, 0, 180, Arc2D.OPEN));
        }
    }

    /** One tile of the kitchen pass counter (steel top, wood front). */
    static void paintPassSegment(Graphics2D g, int i) {
        double gx0 = Layout.KITCHEN_GX + i;
        box(g, gx0, Layout.PASS_GY0, gx0 + 1, Layout.PASS_GY1, Layout.PASS_H, new Color(222, 228, 236));
        AffineTransform saved = g.getTransform();
        g.transform(alongX(iso(gx0, Layout.PASS_GY1)));
        g.setColor(new Color(214, 134, 80));
        g.fill(new Rectangle2D.Double(0, -Layout.PASS_H + 4, 32, Layout.PASS_H - 4));
        g.setColor(new Color(180, 106, 60));
        g.draw(new Rectangle2D.Double(4, -Layout.PASS_H + 8, 24, Layout.PASS_H - 12));
        g.setTransform(saved);
        if (i == 4) {
            Point2D p = up(iso(gx0 + 0.6, (Layout.PASS_GY0 + Layout.PASS_GY1) / 2), Layout.PASS_H);
            g.setColor(new Color(240, 200, 80));
            g.fill(new Arc2D.Double(p.getX() - 7, p.getY() - 10, 14, 13, 0, 180, Arc2D.CHORD));
            g.fill(new Rectangle2D.Double(p.getX() - 9, p.getY() - 4, 18, 3));
        }
    }

    /** A wooden chair seen from the front. The diner sits on it facing the viewer. */
    static void paintChair(Graphics2D g, Point2D seat) {
        double x = seat.getX();
        double y = seat.getY();
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new Ellipse2D.Double(x - 18, y - 6, 36, 12));
        g.setColor(WOOD_DARK);
        g.fill(new RoundRectangle2D.Double(x - 20, y - 78, 40, 9, 6, 6));
        g.fill(new Rectangle2D.Double(x - 18, y - 72, 5, 50));
        g.fill(new Rectangle2D.Double(x + 13, y - 72, 5, 50));
        g.setColor(WOOD);
        g.fill(new Rectangle2D.Double(x - 8, y - 70, 4, 40));
        g.fill(new Rectangle2D.Double(x + 4, y - 70, 4, 40));
        g.setColor(new Color(240, 110, 100));
        g.fill(new Ellipse2D.Double(x - 19, y - 30, 38, 13));
    }

    /** A round dining table with cloth, candle, number card and optional dish. */
    static void paintTable(Graphics2D g, int number, MenuItem dish, boolean emptyPlate, double time) {
        Point2D c = Layout.tableCenter(number);
        double x = c.getX();
        double y = c.getY();
        g.setColor(new Color(0, 0, 0, 45));
        g.fill(new Ellipse2D.Double(x - 40, y - 10, 80, 22));
        g.setColor(WOOD_DARK);
        g.fill(new Rectangle2D.Double(x - 5, y - 24, 10, 22));
        g.fill(new Ellipse2D.Double(x - 20, y - 6, 40, 11));
        double topY = y - 34;
        Path2D cloth = new Path2D.Double();
        cloth.moveTo(x - 44, topY);
        cloth.quadTo(x - 46, topY + 16, x - 38, topY + 20);
        cloth.quadTo(x, topY + 30, x + 38, topY + 20);
        cloth.quadTo(x + 46, topY + 16, x + 44, topY);
        cloth.closePath();
        g.setColor(new Color(255, 252, 246));
        g.fill(cloth);
        g.setColor(new Color(120, 200, 190));
        g.setStroke(new BasicStroke(3f));
        g.draw(new QuadCurve2D.Double(x - 39, topY + 19, x, topY + 29, x + 39, topY + 19));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(x - 44, topY - 18, 88, 36));
        g.setColor(new Color(225, 218, 205));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(x - 44, topY - 18, 88, 36));
        // number flag
        g.setColor(new Color(255, 120, 90));
        g.fill(new RoundRectangle2D.Double(x + 18, topY - 10, 14, 12, 4, 4));
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        drawCentered(g, String.valueOf(number), x + 25, topY - 1);
        // candle
        double cx = x - 26;
        g.setColor(new Color(250, 244, 230));
        g.fill(new Rectangle2D.Double(cx - 2.5, topY - 14, 5, 11));
        double flick = Math.sin(time * 11 + number) * 0.8;
        g.setColor(new Color(255, 196, 70));
        g.fill(new Ellipse2D.Double(cx - 2.5 + flick * 0.4, topY - 22, 5, 9));
        g.setColor(new Color(255, 250, 200));
        g.fill(new Ellipse2D.Double(cx - 1.2 + flick * 0.4, topY - 19, 2.4, 4));
        if (dish != null) {
            DishPainter.paint(g, dish, x - 4, topY - 1, 0.9);
        } else if (emptyPlate) {
            AffineTransform saved = g.getTransform();
            g.translate(x - 4, topY - 1);
            g.scale(0.9, 0.9);
            DishPainter.plate(g);
            g.setTransform(saved);
        }
    }

    static void paintPlant(Graphics2D g, double x, double y, double time) {
        g.setColor(new Color(0, 0, 0, 45));
        g.fill(new Ellipse2D.Double(x - 16, y - 5, 32, 10));
        g.setColor(new Color(240, 140, 90));
        Path2D pot = new Path2D.Double();
        pot.moveTo(x - 13, y - 26);
        pot.lineTo(x + 13, y - 26);
        pot.lineTo(x + 10, y);
        pot.lineTo(x - 10, y);
        pot.closePath();
        g.fill(pot);
        g.setColor(new Color(255, 170, 120));
        g.fill(new Rectangle2D.Double(x - 15, y - 30, 30, 5));
        double sway = Math.sin(time * 1.3 + x) * 1.6;
        Color[] leaves = {new Color(70, 170, 90), new Color(100, 200, 110), new Color(60, 140, 80)};
        for (int i = 0; i < 9; i++) {
            double a = -Math.PI / 2 + (i - 4) * 0.34;
            double len = 26 + (i % 3) * 7;
            double ex = x + Math.cos(a) * len + sway;
            double ey = y - 28 + Math.sin(a) * len;
            g.setColor(leaves[i % 3]);
            Path2D leaf = new Path2D.Double();
            leaf.moveTo(x, y - 28);
            leaf.quadTo((x + ex) / 2 - 7, (y - 28 + ey) / 2, ex, ey);
            leaf.quadTo((x + ex) / 2 + 7, (y - 28 + ey) / 2, x, y - 28);
            g.fill(leaf);
        }
        g.setColor(new Color(255, 120, 150));
        g.fill(new Ellipse2D.Double(x - 6 + sway, y - 58, 7, 7));
        g.fill(new Ellipse2D.Double(x + 8 + sway, y - 50, 6, 6));
    }

    // ----- lighting -----

    /** Evening darkness with warm light from the lamps (cached each turn). */
    static void paintLighting(Graphics2D g, double evening) {
        int dark = (int) (10 + evening * 80);
        g.setColor(new Color(20, 16, 50, dark));
        g.fillRect(0, 0, Layout.SCENE_W, Layout.SCENE_H);
        for (int n = 1; n <= 2; n++) {
            Point2D.Double c = Layout.tableCenter(n);
            glow(g, c.x, c.y - 60, 170, new Color(255, 214, 140), (int) (30 + evening * 70));
        }
        Point2D k = iso(9.5, 0.5);
        glow(g, k.getX(), k.getY() - 60, 150, new Color(255, 246, 220), (int) (25 + evening * 40));
    }

    static void paintCandleGlow(Graphics2D g, double evening, double time) {
        for (int n = 1; n <= 2; n++) {
            Point2D.Double c = Layout.tableCenter(n);
            double flicker = 0.85 + 0.15 * Math.sin(time * 9 + n * 2);
            glow(g, c.x - 26, c.y - 50, 50, new Color(255, 190, 100), (int) ((30 + evening * 70) * flicker));
        }
    }

    static void glow(Graphics2D g, double x, double y, double r, Color c, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        RadialGradientPaint paint = new RadialGradientPaint(new Point2D.Double(x, y), (float) r,
                new float[] {0f, 1f},
                new Color[] {new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha),
                    new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
        g.setPaint(paint);
        Shape s = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        g.fill(s);
    }
}
