package rush.gui;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.Deque;

import rush.domain.MenuItem;

/** A character on screen that walks along a path of waypoints and can show a speech bubble. */
class Actor {

    /** A point to walk to, with an optional action when reached and a pause before moving on. */
    static final class Waypoint {
        final double x;
        final double y;
        final Runnable onArrive;
        final double pause;

        Waypoint(Point2D p, Runnable onArrive, double pause) {
            this.x = p.getX();
            this.y = p.getY();
            this.onArrive = onArrive;
            this.pause = pause;
        }
    }

    /** A short message or dish shown above the character's head. */
    static final class Bubble {
        final String text;
        final MenuItem dish;
        final Color color;
        double ttl;

        Bubble(String text, MenuItem dish, Color color, double ttl) {
            this.text = text;
            this.dish = dish;
            this.color = color;
            this.ttl = ttl;
        }
    }

    final Look look;
    final CharacterPainter.Pose pose = new CharacterPainter.Pose();
    double x;
    double y;
    double speed = 170;
    final Point2D.Double home;

    private final Deque<Waypoint> path = new ArrayDeque<>();
    private double pauseLeft;
    private double blinkTimer = 2 + Math.random() * 3;
    double workTimer;
    double headTop;
    Bubble bubble;
    boolean fadingOut;
    boolean gone;

    Actor(Look look, Point2D start) {
        this.look = look;
        this.x = start.getX();
        this.y = start.getY();
        this.home = new Point2D.Double(start.getX(), start.getY());
    }

    void walkTo(Point2D p) {
        walkTo(p, null, 0);
    }

    void walkTo(Point2D p, Runnable onArrive, double pause) {
        path.add(new Waypoint(p, onArrive, pause));
    }

    void clearPath() {
        path.clear();
        pauseLeft = 0;
    }

    boolean isIdle() {
        return path.isEmpty() && pauseLeft <= 0;
    }

    Point2D.Double destination() {
        Waypoint last = path.peekLast();
        return last == null ? new Point2D.Double(x, y) : new Point2D.Double(last.x, last.y);
    }

    void say(String text, MenuItem dish, Color color, double seconds) {
        bubble = new Bubble(text, dish, color, seconds);
    }

    void update(double dt, double time) {
        pose.time = time;
        if (bubble != null) {
            bubble.ttl -= dt;
            if (bubble.ttl <= 0) {
                bubble = null;
            }
        }
        if (workTimer > 0) {
            workTimer -= dt;
        }
        pose.working = workTimer > 0;
        blinkTimer -= dt;
        if (blinkTimer < 0) {
            pose.blink = true;
            if (blinkTimer < -0.12) {
                pose.blink = false;
                blinkTimer = 2 + Math.random() * 3.5;
            }
        }
        if (fadingOut) {
            pose.alpha = (float) Math.max(0, pose.alpha - dt * 2.5);
            if (pose.alpha <= 0) {
                gone = true;
            }
        } else if (pose.alpha < 1f) {
            pose.alpha = (float) Math.min(1, pose.alpha + dt * 3);
        }
        if (pauseLeft > 0) {
            pauseLeft -= dt;
            pose.moving = false;
            return;
        }
        Waypoint next = path.peekFirst();
        if (next == null) {
            pose.moving = false;
            return;
        }
        double dx = next.x - x;
        double dy = next.y - y;
        double dist = Math.hypot(dx, dy);
        double step = speed * dt;
        if (dist <= step) {
            x = next.x;
            y = next.y;
            path.pollFirst();
            pauseLeft = next.pause;
            if (next.onArrive != null) {
                next.onArrive.run();
            }
            pose.moving = !path.isEmpty() && pauseLeft <= 0;
        } else {
            x += dx / dist * step;
            y += dy / dist * step;
            pose.moving = true;
            pose.walkPhase += dt * 11;
            if (Math.abs(dx) > 1) {
                pose.facing = dx > 0 ? 1 : -1;
            }
        }
    }
}
