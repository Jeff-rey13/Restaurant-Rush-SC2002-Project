package rush.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import rush.domain.MenuItem;

/** Draws a chibi-style character from simple shapes. (x, y) is where the feet are. */
final class CharacterPainter {

    /** How the character is posed in this frame. */
    static final class Pose {
        double walkPhase;
        boolean moving;
        boolean seated;
        int facing = 1;
        /** 0..100 for customers; -1 means a calm staff face. */
        int mood = -1;
        boolean blink;
        /** Item carried on a tray, or null. */
        MenuItem carrying;
        /** Stirring a pan (chef) or writing on a notepad (waiter). */
        boolean working;
        boolean eating;
        double time;
        float alpha = 1f;
    }

    private static final Color OUTLINE = new Color(40, 30, 30, 120);
    private static final Stroke THIN = new BasicStroke(1.2f);

    private CharacterPainter() {
    }

    /** Draws the character and returns the y of the top of the head (for bubbles). */
    static double paint(Graphics2D g, Look look, double x, double y, Pose p) {
        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        if (p.alpha < 1f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, p.alpha)));
        }
        double bob = p.moving ? Math.abs(Math.sin(p.walkPhase)) * 2.5 : Math.sin(p.time * 2.2) * 0.8;
        double torsoTop = (p.seated ? y - 60 : y - 56) - bob;
        double headY = torsoTop - 15;

        if (!p.seated) {
            g.setColor(new Color(0, 0, 0, 55));
            g.fill(new Ellipse2D.Double(x - 18, y - 4, 36, 9));
            paintLegs(g, look, x, y, p);
        }
        if (look.hairStyle == Look.Hair.LONG) {
            g.setColor(look.hair);
            g.fill(new RoundRectangle2D.Double(x - 17, headY - 14, 34, 36, 14, 14));
        }
        paintArmsBehind(g, look, x, torsoTop, p);
        paintTorso(g, look, x, torsoTop, p);
        paintHead(g, look, x, headY, p);
        paintArmsFront(g, look, x, torsoTop, p);

        g.setStroke(oldStroke);
        g.setComposite(oldComposite);
        double hatExtra = look.accessory == Look.Accessory.CHEF_HAT ? 26 : 0;
        return headY - 18 - hatExtra;
    }

    private static void paintLegs(Graphics2D g, Look look, double x, double y, Pose p) {
        double swing = p.moving ? Math.sin(p.walkPhase) * 6 : 0;
        double hip = y - 26;
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(look.pants);
        g.draw(new Line2D.Double(x - 5, hip, x - 5 + swing, y - 5));
        g.draw(new Line2D.Double(x + 5, hip, x + 5 - swing, y - 5));
        g.setColor(new Color(45, 35, 32));
        g.fill(new Ellipse2D.Double(x - 11 + swing + p.facing * 1.5, y - 7, 11, 6));
        g.fill(new Ellipse2D.Double(x - 1 - swing + p.facing * 1.5, y - 7, 11, 6));
    }

    private static void paintTorso(Graphics2D g, Look look, double x, double top, Pose p) {
        RoundRectangle2D body = new RoundRectangle2D.Double(x - 15, top, 30, 34, 14, 14);
        g.setPaint(new GradientPaint((float) x, (float) top, look.outfit,
                (float) x, (float) (top + 34), look.outfit.darker()));
        g.fill(body);
        switch (look.accessory) {
            case WAITER_VEST:
                g.setColor(look.accent);
                Path2D vest = new Path2D.Double();
                vest.moveTo(x - 15, top + 6);
                vest.lineTo(x - 4, top + 6);
                vest.lineTo(x, top + 20);
                vest.lineTo(x + 4, top + 6);
                vest.lineTo(x + 15, top + 6);
                vest.lineTo(x + 15, top + 30);
                vest.quadTo(x + 15, top + 34, x + 10, top + 34);
                vest.lineTo(x - 10, top + 34);
                vest.quadTo(x - 15, top + 34, x - 15, top + 30);
                vest.closePath();
                g.fill(vest);
                g.setColor(new Color(190, 40, 50));
                Path2D bow = new Path2D.Double();
                bow.moveTo(x, top + 4);
                bow.lineTo(x - 6, top + 1);
                bow.lineTo(x - 6, top + 7);
                bow.closePath();
                bow.moveTo(x, top + 4);
                bow.lineTo(x + 6, top + 1);
                bow.lineTo(x + 6, top + 7);
                bow.closePath();
                g.fill(bow);
                break;
            case CHEF_HAT:
                g.setColor(new Color(200, 200, 205));
                for (int i = 0; i < 3; i++) {
                    g.fill(new Ellipse2D.Double(x - 7, top + 9 + i * 7, 3, 3));
                    g.fill(new Ellipse2D.Double(x + 4, top + 9 + i * 7, 3, 3));
                }
                g.setColor(look.accent);
                Path2D scarf = new Path2D.Double();
                scarf.moveTo(x - 8, top + 1);
                scarf.lineTo(x + 8, top + 1);
                scarf.lineTo(x, top + 9);
                scarf.closePath();
                g.fill(scarf);
                break;
            case CASHIER_CAP:
                g.setColor(new Color(250, 250, 245));
                g.fill(new RoundRectangle2D.Double(x + 3, top + 9, 9, 5, 2, 2));
                g.setColor(look.outfit.darker());
                g.fill(new Ellipse2D.Double(x - 2, top + 3, 4, 4));
                break;
            default:
                paintCustomerDetail(g, look, x, top);
                break;
        }
        g.setColor(OUTLINE);
        g.setStroke(THIN);
        g.draw(body);
    }

    /** A collar for everyday clothes; extended in later stages for special customers. */
    private static void paintCustomerDetail(Graphics2D g, Look look, double x, double top) {
        g.setColor(look.outfit.brighter());
        Path2D collar = new Path2D.Double();
        collar.moveTo(x - 7, top + 1);
        collar.lineTo(x, top + 8);
        collar.lineTo(x + 7, top + 1);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(collar);
    }

    private static void paintArmsBehind(Graphics2D g, Look look, double x, double top, Pose p) {
        if (p.seated) {
            return;
        }
        double swing = p.moving ? -Math.sin(p.walkPhase) * 6 : 0;
        double side = -p.facing;
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(look.outfit.darker());
        g.draw(new Line2D.Double(x + side * 13, top + 6, x + side * 16 + swing, top + 26));
        g.setColor(look.skin);
        g.fill(new Ellipse2D.Double(x + side * 16 + swing - 3.5, top + 23, 7, 7));
    }

    private static void paintArmsFront(Graphics2D g, Look look, double x, double top, Pose p) {
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double side = p.facing;
        Color sleeve = look.accessory == Look.Accessory.WAITER_VEST ? look.outfit : look.outfit.darker();
        if (p.seated) {
            double lift = p.eating ? Math.max(0, Math.sin(p.time * 3.0)) * 10 : 0;
            g.setColor(sleeve);
            g.draw(new Line2D.Double(x - 13, top + 6, x - 15, top + 28));
            g.draw(new Line2D.Double(x + 13, top + 6, x + 15, top + 28 - lift));
            g.setColor(look.skin);
            g.fill(new Ellipse2D.Double(x - 18.5, top + 25, 7, 7));
            g.fill(new Ellipse2D.Double(x + 11.5, top + 25 - lift, 7, 7));
            if (p.eating) {
                g.setColor(new Color(200, 200, 210));
                g.setStroke(new BasicStroke(1.8f));
                g.draw(new Line2D.Double(x + 15, top + 27 - lift, x + 13, top + 17 - lift));
            }
            return;
        }
        if (p.carrying != null) {
            g.setColor(sleeve);
            g.draw(new Line2D.Double(x + side * 13, top + 6, x + side * 24, top + 12));
            g.setColor(look.skin);
            g.fill(new Ellipse2D.Double(x + side * 24 - 3.5, top + 8, 7, 7));
            g.setColor(new Color(180, 180, 188));
            g.fill(new Ellipse2D.Double(x + side * 24 - 16, top + 3, 32, 7));
            DishPainter.paint(g, p.carrying, x + side * 24, top + 5, 0.75);
            return;
        }
        if (p.working) {
            double stir = Math.sin(p.time * 9) * 4;
            g.setColor(sleeve);
            g.draw(new Line2D.Double(x + side * 13, top + 6, x + side * 22 + stir, top + 20));
            g.setColor(look.skin);
            g.fill(new Ellipse2D.Double(x + side * 22 + stir - 3.5, top + 16.5, 7, 7));
            if (look.accessory == Look.Accessory.WAITER_VEST) {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(x + side * 18 - 6, top + 8, 12, 15, 3, 3));
                g.setColor(new Color(120, 120, 140));
                g.setStroke(new BasicStroke(1f));
                g.draw(new Line2D.Double(x + side * 18 - 3, top + 13, x + side * 18 + 3, top + 13));
                g.draw(new Line2D.Double(x + side * 18 - 3, top + 17, x + side * 18 + 3, top + 17));
            }
            return;
        }
        double swing = p.moving ? Math.sin(p.walkPhase) * 6 : 0;
        g.setColor(sleeve);
        g.draw(new Line2D.Double(x + side * 13, top + 6, x + side * 16 + swing, top + 26));
        g.setColor(look.skin);
        g.fill(new Ellipse2D.Double(x + side * 16 + swing - 3.5, top + 23, 7, 7));
        if (look.accessory == Look.Accessory.WAITER_VEST && !p.moving) {
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(x + side * 15 - 4, top + 14, 8, 14, 3, 3));
        }
    }

    private static void paintHead(Graphics2D g, Look look, double x, double cy, Pose p) {
        double r = 16;
        g.setColor(look.skin);
        g.fill(new Ellipse2D.Double(x - 4, cy + 10, 8, 8));
        g.fill(new Ellipse2D.Double(x - r - 2, cy - 3, 6, 8));
        g.fill(new Ellipse2D.Double(x + r - 4, cy - 3, 6, 8));
        g.fill(new Ellipse2D.Double(x - r, cy - r, 2 * r, 2 * r));
        g.setColor(OUTLINE);
        g.setStroke(THIN);
        g.draw(new Ellipse2D.Double(x - r, cy - r, 2 * r, 2 * r));
        paintHair(g, look, x, cy, r);
        paintFace(g, look, x, cy, p);
        paintHat(g, look, x, cy, r);
    }

    private static void paintHair(Graphics2D g, Look look, double x, double cy, double r) {
        g.setColor(look.hair);
        switch (look.hairStyle) {
            case SHORT:
                g.fill(new Arc2D.Double(x - r - 1, cy - r - 2, 2 * r + 2, 2 * r - 2, 0, 180, Arc2D.CHORD));
                g.fill(new Ellipse2D.Double(x - r, cy - 8, 6, 9));
                g.fill(new Ellipse2D.Double(x + r - 6, cy - 8, 6, 9));
                break;
            case LONG:
                g.fill(new Arc2D.Double(x - r - 2, cy - r - 3, 2 * r + 4, 2 * r, 0, 180, Arc2D.CHORD));
                g.fill(new Ellipse2D.Double(x - r - 2, cy - 8, 7, 18));
                g.fill(new Ellipse2D.Double(x + r - 5, cy - 8, 7, 18));
                break;
            case BUN:
                g.fill(new Arc2D.Double(x - r - 1, cy - r - 2, 2 * r + 2, 2 * r - 4, 0, 180, Arc2D.CHORD));
                g.fill(new Ellipse2D.Double(x - 8, cy - r - 12, 16, 14));
                break;
            case CURLY:
                for (int i = 0; i < 7; i++) {
                    double a = Math.PI + i * Math.PI / 6;
                    g.fill(new Ellipse2D.Double(x + Math.cos(a) * (r - 2) - 6, cy + Math.sin(a) * (r - 2) - 6, 12, 12));
                }
                break;
            case SPIKY:
                Path2D spikes = new Path2D.Double();
                spikes.moveTo(x - r - 1, cy - 2);
                for (int i = 0; i <= 6; i++) {
                    double px = x - r + i * (2 * r / 6.0);
                    spikes.lineTo(px, cy - r - (i % 2 == 0 ? 2 : 9));
                }
                spikes.lineTo(x + r + 1, cy - 2);
                spikes.quadTo(x, cy - 8, x - r - 1, cy - 2);
                g.fill(spikes);
                break;
            case BALD:
            default:
                g.fill(new Ellipse2D.Double(x - r, cy - 4, 5, 8));
                g.fill(new Ellipse2D.Double(x + r - 5, cy - 4, 5, 8));
                break;
        }
    }

    private static void paintFace(Graphics2D g, Look look, double x, double cy, Pose p) {
        double fx = x + p.facing * 2;
        g.setColor(new Color(40, 30, 30));
        if (p.blink) {
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(fx - 7, cy + 1, fx - 3, cy + 1));
            g.draw(new Line2D.Double(fx + 3, cy + 1, fx + 7, cy + 1));
        } else {
            g.fill(new Ellipse2D.Double(fx - 7, cy - 2, 4, 5.5));
            g.fill(new Ellipse2D.Double(fx + 3, cy - 2, 4, 5.5));
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(fx - 6, cy - 1.5, 1.6, 1.6));
            g.fill(new Ellipse2D.Double(fx + 4, cy - 1.5, 1.6, 1.6));
        }
        int mood = p.mood < 0 ? 75 : p.mood;
        if (mood >= 65) {
            g.setColor(new Color(240, 120, 120, 110));
            g.fill(new Ellipse2D.Double(fx - 12, cy + 3, 6, 4));
            g.fill(new Ellipse2D.Double(fx + 6, cy + 3, 6, 4));
        }
        g.setColor(new Color(110, 40, 40));
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (mood >= 65) {
            g.draw(new Arc2D.Double(fx - 5, cy + 1, 10, 8, 200, 140, Arc2D.OPEN));
        } else if (mood >= 35) {
            g.draw(new Line2D.Double(fx - 3.5, cy + 7, fx + 3.5, cy + 7));
        } else {
            g.draw(new Arc2D.Double(fx - 5, cy + 6, 10, 7, 20, 140, Arc2D.OPEN));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(fx - 8, cy - 6, fx - 3, cy - 4));
            g.draw(new Line2D.Double(fx + 8, cy - 6, fx + 3, cy - 4));
        }
    }

    private static void paintHat(Graphics2D g, Look look, double x, double cy, double r) {
        if (look.accessory == Look.Accessory.CHEF_HAT) {
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Double(x - 12, cy - r - 12, 24, 14, 4, 4));
            g.fill(new Ellipse2D.Double(x - 17, cy - r - 30, 18, 20));
            g.fill(new Ellipse2D.Double(x - 1, cy - r - 30, 18, 20));
            g.fill(new Ellipse2D.Double(x - 9, cy - r - 36, 18, 20));
            g.setColor(new Color(210, 210, 215));
            g.setStroke(THIN);
            g.draw(new Line2D.Double(x - 12, cy - r + 1, x + 12, cy - r + 1));
        } else if (look.accessory == Look.Accessory.CASHIER_CAP) {
            g.setColor(look.accent);
            g.fill(new Arc2D.Double(x - r, cy - r - 3, 2 * r, 2 * r - 4, 0, 180, Arc2D.CHORD));
            g.fill(new RoundRectangle2D.Double(x - 2, cy - 7, r + 8, 5, 4, 4));
        }
    }
}
