package rush.gui;

import java.awt.geom.Point2D;

/**
 * Isometric layout of the restaurant on a 1280 x 720 canvas. The floor is a
 * grid of GX x GY tiles. iso(gx, gy) turns a grid position into the screen
 * point where a character's feet (or an object's base) go.
 */
final class Layout {

    static final int W = 1280;
    static final int H = 720;

    /** Restaurant scene: the left part of the canvas. */
    static final int SCENE_W = 900;
    static final int SCENE_H = 560;
    /** Action bar below the scene, and side panel on its right. */
    static final int BAR_Y = SCENE_H;
    static final int SIDE_X = SCENE_W;

    /** Tile size and grid. */
    static final double TW = 64;
    static final double TH = 32;
    static final int GX = 12;
    static final int GY = 10;
    /** Screen position of the back corner where the two walls meet. */
    static final double OX = 418;
    static final double OY = 170;
    static final double WALL_H = 140;
    /** The kitchen covers gx >= KITCHEN_GX and gy < PASS_GY. */
    static final double KITCHEN_GX = 7;
    static final double PASS_GY0 = 2.0;
    static final double PASS_GY1 = 2.6;
    static final double PASS_H = 26;

    static final Point2D.Double DOOR_OUTSIDE = iso(-0.4, 6.9);
    static final Point2D.Double DOOR_INSIDE = iso(0.7, 6.9);
    static final Point2D.Double CASHIER_SPOT = iso(0.45, 2.1);
    static final Point2D.Double PAY_SPOT = iso(2.5, 2.1);
    static final Point2D.Double CHEF_SPOT = iso(9.5, 1.45);
    static final Point2D.Double WAITER_HOME = iso(5.4, 3.3);
    static final Point2D.Double PASS_PICKUP = iso(8.6, 3.25);
    static final double[] PODIUM = {1.45, 4.75};
    static final Point2D.Double HOST_SPOT = iso(0.85, 4.3);

    private static final double[][] TABLES = {{4.3, 7.3}, {8.0, 5.0}};

    private Layout() {
    }

    static Point2D.Double iso(double gx, double gy) {
        return new Point2D.Double(OX + (gx - gy) * TW / 2, OY + (gx + gy) * TH / 2);
    }

    /** Grid position of the centre of a table. */
    static double[] tableGrid(int tableNumber) {
        return TABLES[(tableNumber - 1) % TABLES.length];
    }

    static Point2D.Double tableCenter(int tableNumber) {
        double[] t = tableGrid(tableNumber);
        return iso(t[0], t[1]);
    }

    /** Grid position of the chair, straight behind the table on screen. */
    static double[] seatGrid(int tableNumber) {
        double[] t = tableGrid(tableNumber);
        return new double[] {t[0] - 0.85, t[1] - 0.85};
    }

    /** The diner sits behind the table facing the viewer, so the table is in front. */
    static Point2D.Double seat(int tableNumber) {
        double[] s = seatGrid(tableNumber);
        return iso(s[0], s[1]);
    }

    /** Where staff stand to serve: in front of the table, to the right. */
    static Point2D.Double tableSide(int tableNumber) {
        double[] t = tableGrid(tableNumber);
        return iso(t[0] + 0.5, t[1] + 1.15);
    }

    /** Queue spots go from the door along the left wall towards the front. */
    static Point2D.Double queueSlot(int index) {
        double[][] slots = {{1.5, 6.6}, {1.5, 8.1}, {2.3, 9.4}, {3.8, 9.4}, {5.3, 9.4}, {6.8, 9.4}};
        double[] s = slots[Math.min(index, slots.length - 1)];
        return iso(s[0], s[1]);
    }

    /** Spots on the kitchen pass where READY dishes wait. */
    static Point2D.Double passSpot(int index) {
        Point2D.Double p = iso(7.7 + index * 1.0, (PASS_GY0 + PASS_GY1) / 2);
        p.y -= PASS_H;
        return p;
    }
}
