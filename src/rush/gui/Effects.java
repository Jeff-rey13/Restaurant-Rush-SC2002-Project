package rush.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Short-lived visual effects: floating numbers, steam, coins, sparkles and puffs. */
final class Effects {

    private static final class FloatText {
        String text;
        Color color;
        double x;
        double y;
        double life;
        double ttl;
    }

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double life;
        double ttl;
        double size;
        Color color;
        boolean gravity;
        boolean soft;
    }

    private final List<FloatText> texts = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random(7);
    private double steamTimer;

    void floatText(String text, Color color, double x, double y) {
        FloatText t = new FloatText();
        t.text = text;
        t.color = color;
        t.x = x;
        t.y = y;
        t.ttl = 1.8;
        texts.add(t);
    }

    void steam(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x + random.nextGaussian() * 6;
            p.y = y;
            p.vx = random.nextGaussian() * 4;
            p.vy = -20 - random.nextDouble() * 18;
            p.ttl = 1.6 + random.nextDouble();
            p.size = 8 + random.nextDouble() * 8;
            p.color = new Color(255, 255, 255);
            p.soft = true;
            particles.add(p);
        }
    }

    void flames(double x, double y) {
        for (int i = 0; i < 6; i++) {
            Particle p = new Particle();
            p.x = x + random.nextGaussian() * 9;
            p.y = y;
            p.vx = random.nextGaussian() * 6;
            p.vy = -40 - random.nextDouble() * 30;
            p.ttl = 0.4 + random.nextDouble() * 0.3;
            p.size = 5 + random.nextDouble() * 6;
            p.color = random.nextBoolean() ? new Color(255, 170, 40) : new Color(255, 90, 30);
            particles.add(p);
        }
    }

    void coins(double x, double y) {
        for (int i = 0; i < 14; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = random.nextGaussian() * 50;
            p.vy = -120 - random.nextDouble() * 80;
            p.ttl = 1.1;
            p.size = 7;
            p.color = new Color(250, 205, 60);
            p.gravity = true;
            particles.add(p);
        }
    }

    void puff(double x, double y, Color color) {
        for (int i = 0; i < 10; i++) {
            Particle p = new Particle();
            p.x = x + random.nextGaussian() * 10;
            p.y = y + random.nextGaussian() * 4;
            p.vx = random.nextGaussian() * 25;
            p.vy = -15 - random.nextDouble() * 20;
            p.ttl = 0.9;
            p.size = 10 + random.nextDouble() * 8;
            p.color = color;
            particles.add(p);
        }
    }

    void confetti(double width) {
        Color[] colors = {new Color(250, 200, 60), new Color(240, 90, 90), new Color(90, 190, 120),
            new Color(90, 150, 240), new Color(220, 120, 220)};
        for (int i = 0; i < 120; i++) {
            Particle p = new Particle();
            p.x = random.nextDouble() * width;
            p.y = -20 - random.nextDouble() * 200;
            p.vx = random.nextGaussian() * 20;
            p.vy = 60 + random.nextDouble() * 80;
            p.ttl = 6;
            p.size = 6;
            p.color = colors[i % colors.length];
            particles.add(p);
        }
    }

    /** Kitchen pots steam gently all the time. */
    void ambient(double dt) {
        steamTimer -= dt;
        if (steamTimer <= 0) {
            steamTimer = 0.35;
            steam(World.POT.x - 14, World.POT.y - 30, 1);
            if (random.nextBoolean()) {
                steam(World.PAN.x + 10, World.PAN.y - 8, 1);
            }
        }
    }

    void update(double dt) {
        for (Iterator<FloatText> it = texts.iterator(); it.hasNext();) {
            FloatText t = it.next();
            t.life += dt;
            t.y -= 22 * dt;
            if (t.life >= t.ttl) {
                it.remove();
            }
        }
        for (Iterator<Particle> it = particles.iterator(); it.hasNext();) {
            Particle p = it.next();
            p.life += dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.gravity) {
                p.vy += 380 * dt;
            }
            if (p.life >= p.ttl) {
                it.remove();
            }
        }
    }

    void paintParticles(Graphics2D g) {
        Composite saved = g.getComposite();
        for (Particle p : particles) {
            float a = (float) Math.max(0, 1 - p.life / p.ttl);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.soft ? a * 0.35f : a));
            g.setColor(p.color);
            double s = p.soft ? p.size * (1 + p.life) : p.size;
            if (p.gravity) {
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s * 0.8));
                g.setColor(new Color(200, 150, 30));
                g.fill(new Ellipse2D.Double(p.x - s / 4, p.y - s / 4, s / 2, s / 2.5));
            } else {
                g.fill(new Ellipse2D.Double(p.x - s / 2, p.y - s / 2, s, s));
            }
        }
        g.setComposite(saved);
    }

    void paintTexts(Graphics2D g) {
        Composite saved = g.getComposite();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        for (FloatText t : texts) {
            float a = (float) Math.max(0, Math.min(1, (t.ttl - t.life) / 0.6));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.setColor(new Color(0, 0, 0, 150));
            SceneRenderer.drawCentered(g, t.text, t.x + 1, t.y + 1);
            g.setColor(t.color);
            SceneRenderer.drawCentered(g, t.text, t.x, t.y);
        }
        g.setComposite(saved);
    }
}
