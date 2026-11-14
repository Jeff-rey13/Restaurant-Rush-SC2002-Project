package rush.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import rush.control.GameController;
import rush.domain.ActionResult;
import rush.domain.Customer;
import rush.domain.CustomerStatus;
import rush.domain.MenuItem;
import rush.domain.Money;
import rush.domain.Order;
import rush.domain.OrderStatus;
import rush.domain.Restaurant;
import rush.domain.Table;

/**
 * Everything that moves on screen. Each frame it compares the game state with
 * what it showed last time and animates the difference, so the game logic
 * never needs to know it is being drawn.
 */
final class World {

    static final Color GOOD = new Color(110, 220, 120);
    static final Color BAD = new Color(255, 110, 100);
    static final Color GOLD = new Color(255, 214, 100);

    /** A customer on screen and what was last shown for them. */
    static final class CustomerActor extends Actor {
        final Customer customer;
        CustomerStatus shownStatus;
        int shownSatisfaction;
        boolean dishOnTable;
        double eatingFor;
        boolean leaving;

        CustomerActor(Customer customer, Point2D start) {
            super(Appearances.forCustomer(customer), start);
            this.customer = customer;
            this.shownSatisfaction = customer.getSatisfaction();
            this.speed = 150;
        }
    }

    private interface Drawable {
        void draw(Graphics2D g);
    }

    final Effects effects = new Effects();
    private final Map<Customer, CustomerActor> customers = new LinkedHashMap<>();
    private final Map<Order, Integer> unitsShown = new HashMap<>();
    final Actor waiter = new Actor(Appearances.waiter(), Layout.WAITER_HOME);
    final Actor chef = new Actor(Appearances.chef(), Layout.CHEF_SPOT);
    final Actor cashier = new Actor(Appearances.cashier(), Layout.CASHIER_SPOT);

    static final Point2D.Double POT = SceneRenderer.up(Layout.iso(Layout.KITCHEN_GX + 0.5, 0.48), 38);
    static final Point2D.Double PAN = SceneRenderer.up(Layout.iso(Layout.KITCHEN_GX + 1.5, 0.48), 38);
    private static final double[][] PLANTS = {{0.55, 5.2}, {6.4, 0.45}, {11.4, 9.4}, {0.55, 9.4}};
    private final SceneCache roomCache = new SceneCache();
    private final SceneCache lightCache = new SceneCache();
    private Order carriedOrder;
    private boolean carriedPicked;
    private double doorOpen;
    private double time;

    World() {
        chef.pose.facing = 1;
        cashier.pose.facing = 1;
        waiter.pose.facing = -1;
    }

    void reset() {
        customers.clear();
        unitsShown.clear();
        carriedOrder = null;
        waiter.clearPath();
        waiter.x = Layout.WAITER_HOME.x;
        waiter.y = Layout.WAITER_HOME.y;
        waiter.pose.carrying = null;
    }

    // ------------------------------------------------------------------ sync

    /** Compares the game state with the screen and starts animations for whatever changed. */
    void sync(GameController game) {
        if (game == null) {
            return;
        }
        Restaurant r = game.getRestaurant();
        List<Customer> queue = r.getWaitingQueue();
        for (Customer c : r.getCustomers()) {
            CustomerActor a = customers.get(c);
            if (a == null) {
                if (c.getStatus() != CustomerStatus.WAITING) {
                    continue;
                }
                a = arrive(c, queue.indexOf(c));
            }
            CustomerStatus now = c.getStatus();
            if (now != a.shownStatus) {
                transition(a, now);
                a.shownStatus = now;
            }
            if (now == CustomerStatus.WAITING && a.isIdle()) {
                Point2D slot = Layout.queueSlot(queue.indexOf(c));
                if (Math.hypot(slot.getX() - a.x, slot.getY() - a.y) > 1) {
                    a.walkTo(slot);
                }
            }
            int sat = c.getSatisfaction();
            if (sat != a.shownSatisfaction) {
                int delta = sat - a.shownSatisfaction;
                effects.floatText((delta > 0 ? "+" : "") + delta, delta > 0 ? GOOD : BAD, a.x + 22, a.headTop + 6);
                a.shownSatisfaction = sat;
            }
        }
        for (Order o : r.getActiveOrders()) {
            Integer before = unitsShown.get(o);
            if (before != null && o.getUnitsDone() > before && o.getStatus() == OrderStatus.READY) {
                SoundFx.play(SoundFx.Sound.DING);
                Point2D spot = Layout.passSpot(0);
                effects.floatText("Order up!", GOLD, spot.getX() + 40, spot.getY() - 30);
                effects.steam(spot.getX(), spot.getY() - 10, 4);
            }
            unitsShown.put(o, o.getUnitsDone());
        }
    }

    private CustomerActor arrive(Customer c, int slot) {
        CustomerActor a = new CustomerActor(c, Layout.DOOR_OUTSIDE);
        a.pose.alpha = 0f;
        a.shownStatus = CustomerStatus.WAITING;
        a.walkTo(Layout.DOOR_INSIDE);
        a.walkTo(Layout.queueSlot(Math.max(0, slot)));
        a.say(Appearances.styleOf(c).greeting, null, Color.WHITE, 2.6);
        customers.put(c, a);
        doorOpen = 1.4;
        SoundFx.play(SoundFx.Sound.DOOR);
        return a;
    }

    private void transition(CustomerActor a, CustomerStatus now) {
        Customer c = a.customer;
        switch (now) {
            case SEATED:
                Table t = c.getTable();
                a.clearPath();
                a.walkTo(Layout.seat(t.getNumber()), () -> {
                    a.pose.seated = true;
                    a.pose.facing = 1;
                }, 0);
                break;
            case ORDERED:
                MenuItem item = c.getOrder().getItem();
                a.say("The " + item.getName() + ", please!", item, Color.WHITE, 3.2);
                SoundFx.play(SoundFx.Sound.POP);
                break;
            case EATING:
                a.dishOnTable = false;
                a.eatingFor = 0;
                int reaction = c.getSatisfaction() - a.shownSatisfaction;
                if (reaction > 0) {
                    a.say("Impressive service! (+" + reaction + ")", null, new Color(200, 255, 200), 3.0);
                    effects.confettiBurst(a.x, a.y - 80);
                } else if (reaction < 0) {
                    a.say("That took far too long... (" + reaction + ")", null, new Color(255, 200, 190), 3.0);
                    effects.puff(a.x, a.y - 70, new Color(90, 80, 80));
                }
                break;
            case READY_TO_PAY:
                a.say("Delicious! Check, please.", null, GOLD, 2.8);
                break;
            case PAID:
                leave(a, true);
                break;
            case LEFT:
                leave(a, false);
                break;
            default:
                break;
        }
    }

    private void leave(CustomerActor a, boolean paid) {
        a.leaving = true;
        a.pose.seated = false;
        a.clearPath();
        if (paid) {
            int amount = a.customer.getOrder().getPriceCents();
            a.walkTo(Layout.PAY_SPOT, () -> {
                SoundFx.play(SoundFx.Sound.CASH);
                effects.coins(Layout.PAY_SPOT.x, Layout.PAY_SPOT.y - 70);
                effects.floatText("+" + Money.format(amount), GOLD, Layout.PAY_SPOT.x, Layout.PAY_SPOT.y - 92);
                cashier.say("Thank you!", null, Color.WHITE, 1.6);
            }, 0.7);
        } else {
            a.say("Hmph! Too slow. I'm leaving!", null, BAD, 2.6);
            effects.puff(a.x, a.y - 60, new Color(90, 80, 80));
            SoundFx.play(SoundFx.Sound.LEAVE);
            a.speed = 210;
        }
        a.walkTo(Layout.DOOR_INSIDE, () -> doorOpen = 1.2, 0);
        a.walkTo(Layout.DOOR_OUTSIDE, () -> a.fadingOut = true, 0);
    }

    // ------------------------------------------------------- staff actions

    /** Animates a staff member carrying out (or failing) the manager's choice. */
    void onStaffAction(TurnFlow.Option option, ActionResult result) {
        Actor staff = option.role == TurnFlow.Role.CHEF ? chef : waiter;
        if (!result.isSuccess()) {
            staff.say("Can't do that!", null, BAD, 1.6);
            SoundFx.play(SoundFx.Sound.ERROR);
            return;
        }
        CustomerActor target = option.customer == null ? null : customers.get(option.customer);
        switch (option.kind) {
            case SEAT:
                resetWaiter();
                if (target != null) {
                    waiter.walkTo(new Point2D.Double(target.x + 40, target.y));
                }
                waiter.walkTo(Layout.tableSide(option.customer.getTable().getNumber()));
                waiter.walkTo(Layout.WAITER_HOME);
                waiter.say("Right this way!", null, Color.WHITE, 1.8);
                break;
            case TAKE_ORDER:
                resetWaiter();
                int table = option.customer.getTable().getNumber();
                waiter.walkTo(Layout.tableSide(table), () -> waiter.workTimer = 1.2, 1.2);
                waiter.walkTo(Layout.PASS_PICKUP, null, 0.2);
                waiter.walkTo(Layout.WAITER_HOME);
                break;
            case SERVE:
                resetWaiter();
                Order order = option.order;
                carriedOrder = order;
                carriedPicked = false;
                waiter.walkTo(Layout.PASS_PICKUP, () -> {
                    carriedPicked = true;
                    waiter.pose.carrying = order.getItem();
                }, 0.25);
                waiter.walkTo(Layout.tableSide(order.getCustomer().getTable().getNumber()), () -> {
                    waiter.pose.carrying = null;
                    carriedOrder = null;
                    if (target != null) {
                        target.dishOnTable = true;
                    }
                    SoundFx.play(SoundFx.Sound.POP);
                    waiter.say("Enjoy your meal!", null, Color.WHITE, 1.6);
                }, 0.4);
                waiter.walkTo(Layout.WAITER_HOME);
                break;
            case COOK:
                chef.workTimer = 1.8;
                effects.flames(PAN.x + 10, PAN.y - 6);
                SoundFx.play(SoundFx.Sound.SIZZLE);
                break;
            case WAIT:
            default:
                staff.say("...", null, Color.WHITE, 1.2);
                break;
        }
    }

    private void resetWaiter() {
        waiter.clearPath();
        waiter.pose.carrying = null;
        if (carriedOrder != null) {
            CustomerActor a = customers.get(carriedOrder.getCustomer());
            if (a != null) {
                a.dishOnTable = true;
            }
            carriedOrder = null;
        }
    }

    // ---------------------------------------------------------------- update

    void update(double dt) {
        time += dt;
        doorOpen = Math.max(0, doorOpen - dt);
        effects.ambient(dt);
        effects.update(dt);
        waiter.update(dt, time);
        chef.update(dt, time);
        cashier.update(dt, time);
        if (chef.pose.working && Math.random() < dt * 8) {
            effects.flames(PAN.x + 10, PAN.y - 6);
        }
        if (waiter.isIdle()) {
            waiter.pose.facing = -1;
        }
        for (Iterator<CustomerActor> it = customers.values().iterator(); it.hasNext();) {
            CustomerActor a = it.next();
            a.update(dt, time);
            if (a.shownStatus == CustomerStatus.EATING && !a.dishOnTable) {
                a.eatingFor += dt;
                if (a.eatingFor > 4) {
                    a.dishOnTable = true;
                }
            }
            if (a.gone) {
                it.remove();
            }
        }
    }

    // ---------------------------------------------------------------- paint

    void paint(Graphics2D g, GameController game, double evening) {
        List<MenuItem> menu = game.getRestaurant().getMenu().getItems();
        int turn = game.getCurrentTurn();
        int total = game.getConfig().getTotalTurns();
        double deviceScale = g.getTransform().getScaleX();
        roomCache.draw(g, evening, deviceScale, menu.size() * 100 + turn,
                cg -> SceneRenderer.paintStaticRoom(cg, evening, turn, total, menu));
        SceneRenderer.paintAnimatedRoom(g, evening, time, Math.min(1, doorOpen * 2));
        List<double[]> keys = new ArrayList<>();
        List<Drawable> drawables = new ArrayList<>();
        Restaurant r = game.getRestaurant();
        add(keys, drawables, cashier.y, gg -> paintActor(gg, cashier, -1, false));
        for (int seg = 0; seg < 2; seg++) {
            int s2 = seg;
            add(keys, drawables, Layout.iso(1.4, seg == 0 ? 1.55 : 2.65).y, gg -> SceneRenderer.paintCashierSegment(gg, s2));
        }
        for (int i = 0; i < 5; i++) {
            int k = i;
            add(keys, drawables, Layout.iso(Layout.KITCHEN_GX + i + 0.5, 0.48).y,
                    gg -> SceneRenderer.paintKitchenSegment(gg, k, time, chef.pose.working));
            double passKey = Layout.iso(Layout.KITCHEN_GX + i + 0.5, 2.3).y;
            add(keys, drawables, passKey, gg -> SceneRenderer.paintPassSegment(gg, k));
            add(keys, drawables, passKey + 0.5, gg -> paintPassDish(gg, r, k));
        }
        add(keys, drawables, chef.y, gg -> paintActor(gg, chef, -1, false));
        for (Table t : r.getTables()) {
            Point2D.Double seat = Layout.seat(t.getNumber());
            add(keys, drawables, seat.y - 0.5, gg -> SceneRenderer.paintChair(gg, seat));
            add(keys, drawables, Layout.tableCenter(t.getNumber()).y, gg -> paintTable(gg, t));
        }
        for (double[] plant : PLANTS) {
            Point2D.Double p = Layout.iso(plant[0], plant[1]);
            add(keys, drawables, p.y, gg -> SceneRenderer.paintPlant(gg, p.x, p.y, time));
        }
        for (CustomerActor a : customers.values()) {
            add(keys, drawables, a.y, gg -> paintCustomer(gg, a));
        }
        add(keys, drawables, waiter.y, gg -> paintActor(gg, waiter, -1, false));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < drawables.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> Double.compare(keys.get(a)[0], keys.get(b)[0]));
        for (int i : order) {
            drawables.get(i).draw(g);
        }
        lightCache.draw(g, evening, deviceScale, 0, cg -> SceneRenderer.paintLighting(cg, evening));
        SceneRenderer.paintCandleGlow(g, evening, time);
        effects.paintParticles(g);
        paintTicketRail(g, game.getRestaurant());
        for (CustomerActor a : customers.values()) {
            if (!a.leaving) {
                paintStatus(g, a);
            }
        }
        for (Actor a : allActors()) {
            paintBubble(g, a);
        }
        effects.paintTexts(g);
    }

    private static void add(List<double[]> keys, List<Drawable> list, double y, Drawable d) {
        keys.add(new double[] {y});
        list.add(d);
    }

    private List<Actor> allActors() {
        List<Actor> all = new ArrayList<>(customers.values());
        all.add(waiter);
        all.add(chef);
        all.add(cashier);
        return all;
    }

    private void paintActor(Graphics2D g, Actor a, int mood, boolean eating) {
        a.pose.mood = mood;
        a.pose.eating = eating;
        a.headTop = CharacterPainter.paint(g, a.look, a.x, a.y, a.pose);
    }

    private void paintCustomer(Graphics2D g, CustomerActor a) {
        boolean eating = a.shownStatus == CustomerStatus.EATING && a.dishOnTable;
        int mood = a.leaving && a.customer.getStatus() == CustomerStatus.PAID ? 100 : a.customer.getSatisfaction();
        paintActor(g, a, mood, eating);
    }

    private void paintTable(Graphics2D g, Table t) {
        MenuItem dish = null;
        boolean empty = false;
        Customer occupant = t.getOccupant();
        if (occupant != null) {
            CustomerActor a = customers.get(occupant);
            if (occupant.getStatus() == CustomerStatus.EATING && a != null && a.dishOnTable) {
                dish = occupant.getOrder().getItem();
            } else if (occupant.getStatus() == CustomerStatus.READY_TO_PAY) {
                empty = true;
            }
        }
        SceneRenderer.paintTable(g, t.getNumber(), dish, empty, time);
    }

    /** Draws the dish waiting on pass spot k, if there is one. */
    private void paintPassDish(Graphics2D g, Restaurant r, int k) {
        List<MenuItem> waiting = new ArrayList<>();
        for (Order o : r.getActiveOrders()) {
            if (o.getStatus() == OrderStatus.READY) {
                waiting.add(o.getItem());
            }
        }
        if (carriedOrder != null && !carriedPicked) {
            waiting.add(carriedOrder.getItem());
        }
        if (k < waiting.size()) {
            Point2D p = Layout.passSpot(k);
            DishPainter.paint(g, waiting.get(k), p.getX(), p.getY(), 0.8);
        }
    }

    /** Order tickets clipped to a rail above the kitchen, with cooking progress. */
    private void paintTicketRail(Graphics2D g, Restaurant r) {
        int x0 = 690;
        g.setColor(new Color(150, 156, 164));
        g.fill(new RoundRectangle2D.Double(x0, 10, Layout.SCENE_W - x0 - 8, 5, 3, 3));
        int i = 0;
        for (Order o : r.getActiveOrders()) {
            if (i >= 2) {
                break;
            }
            double tx = x0 + 6 + i * 98;
            double sway = Math.sin(time * 1.5 + i) * 0.8;
            g.setColor(new Color(0, 0, 0, 60));
            g.fill(new java.awt.geom.Rectangle2D.Double(tx + 2 + sway, 16, 90, 50));
            g.setColor(new Color(252, 248, 232));
            g.fill(new java.awt.geom.Rectangle2D.Double(tx + sway, 14, 90, 50));
            g.setColor(new Color(170, 170, 176));
            g.fill(new java.awt.geom.Rectangle2D.Double(tx + 38 + sway, 9, 14, 8));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g.setColor(new Color(60, 40, 30));
            g.drawString(o.getLabel().replace("Order ", "") + "  " + TurnFlow.name(o.getCustomer()), (float) (tx + 5 + sway), 28);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.drawString(o.getItem().getName(), (float) (tx + 5 + sway), 42);
            int units = o.getItem().getPrepUnits();
            for (int u = 0; u < units; u++) {
                boolean done = u < o.getUnitsDone();
                g.setColor(done ? new Color(90, 170, 90) : new Color(210, 205, 190));
                g.fill(new RoundRectangle2D.Double(tx + 5 + sway + u * 16, 49, 13, 9, 3, 3));
            }
            if (o.getStatus() == OrderStatus.READY) {
                g.setColor(new Color(200, 60, 50));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                g.drawString("READY", (float) (tx + 52 + sway), 58);
            }
            i++;
        }
    }

    /** Name, mood bar and a small icon for what the customer is waiting for. */
    private void paintStatus(Graphics2D g, CustomerActor a) {
        Customer c = a.customer;
        double cx = a.x;
        double top = a.headTop - 4;
        int sat = c.getSatisfaction();
        Color bar = sat >= 65 ? new Color(90, 200, 100) : sat >= 35 ? new Color(240, 190, 60) : new Color(230, 80, 70);
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new RoundRectangle2D.Double(cx - 21, top - 8, 42, 8, 4, 4));
        g.setColor(bar);
        g.fill(new RoundRectangle2D.Double(cx - 20, top - 7, 40 * sat / 100.0, 6, 3, 3));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        String label = TurnFlow.name(c);
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(label) + 10;
        g.setColor(new Color(20, 16, 14, 170));
        g.fill(new RoundRectangle2D.Double(cx - w / 2, top - 24, w, 15, 8, 8));
        g.setColor(Color.WHITE);
        SceneRenderer.drawCentered(g, label, cx, top - 12.5);
        paintStatusIcon(g, c, cx + w / 2 + 9, top - 16);
        Color badge = Appearances.styleOf(c).badge;
        if (badge != null) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            String type = c.getTypeName().toUpperCase();
            double bw = g.getFontMetrics().stringWidth(type) + 10;
            g.setColor(badge);
            g.fill(new RoundRectangle2D.Double(cx - w / 2 - bw + 4, top - 30, bw, 13, 7, 7));
            g.setColor(Color.WHITE);
            SceneRenderer.drawCentered(g, type, cx - w / 2 - bw / 2 + 4, top - 20.5);
        }
    }

    private void paintStatusIcon(Graphics2D g, Customer c, double x, double y) {
        g.setColor(new Color(255, 255, 255, 230));
        g.fill(new Ellipse2D.Double(x - 8, y - 8, 16, 16));
        g.setStroke(new BasicStroke(1.6f));
        switch (c.getStatus()) {
            case WAITING:
                g.setColor(new Color(150, 110, 60));
                Path2D glass = new Path2D.Double();
                glass.moveTo(x - 4.5, y - 5.5);
                glass.lineTo(x + 4.5, y - 5.5);
                glass.lineTo(x + 0.8, y);
                glass.lineTo(x + 4.5, y + 5.5);
                glass.lineTo(x - 4.5, y + 5.5);
                glass.lineTo(x - 0.8, y);
                glass.closePath();
                g.draw(glass);
                g.fill(new java.awt.geom.Rectangle2D.Double(x - 2.5, y + 2.5, 5, 2.5));
                break;
            case SEATED:
                g.setColor(new Color(60, 90, 160));
                g.drawRect((int) x - 4, (int) y - 5, 8, 10);
                g.drawLine((int) x - 2, (int) y - 2, (int) x + 2, (int) y - 2);
                g.drawLine((int) x - 2, (int) y + 1, (int) x + 2, (int) y + 1);
                break;
            case ORDERED:
                Order o = c.getOrder();
                g.setColor(new Color(220, 110, 40));
                double done = o.getUnitsDone() / (double) o.getItem().getPrepUnits();
                if (o.getStatus() == OrderStatus.READY) {
                    done = 1;
                }
                g.fill(new java.awt.geom.Arc2D.Double(x - 6, y - 6, 12, 12, 90, -360 * done, java.awt.geom.Arc2D.PIE));
                g.draw(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
                break;
            case EATING:
                g.setColor(new Color(90, 150, 90));
                g.drawLine((int) x - 3, (int) y - 5, (int) x - 3, (int) y + 5);
                g.drawLine((int) x + 3, (int) y - 5, (int) x + 3, (int) y + 5);
                break;
            case READY_TO_PAY:
                g.setColor(new Color(200, 150, 20));
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                SceneRenderer.drawCentered(g, "$", x, y + 4.5);
                break;
            default:
                break;
        }
    }

    private void paintBubble(Graphics2D g, Actor a) {
        Actor.Bubble b = a.bubble;
        if (b == null || a.gone) {
            return;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        double iconW = b.dish != null ? 34 : 0;
        double w = fm.stringWidth(b.text) + 18 + iconW;
        double h = b.dish != null ? 34 : 24;
        double bx = Math.max(6, Math.min(Layout.SCENE_W - w - 6, a.x - w / 2));
        double by = a.headTop - (a instanceof CustomerActor && !((CustomerActor) a).leaving ? 32 : 6) - h - 8;
        float alpha = (float) Math.min(1, b.ttl * 3);
        g.setColor(new Color(0, 0, 0, (int) (60 * alpha)));
        g.fill(new RoundRectangle2D.Double(bx + 2, by + 3, w, h, 14, 14));
        g.setColor(new Color(b.color.getRed(), b.color.getGreen(), b.color.getBlue(), (int) (245 * alpha)));
        g.fill(new RoundRectangle2D.Double(bx, by, w, h, 14, 14));
        Path2D tail = new Path2D.Double();
        double tx = Math.max(bx + 10, Math.min(bx + w - 10, a.x));
        tail.moveTo(tx - 6, by + h - 1);
        tail.lineTo(tx + 6, by + h - 1);
        tail.lineTo(tx, by + h + 8);
        tail.closePath();
        g.fill(tail);
        if (b.dish != null) {
            DishPainter.paint(g, b.dish, bx + 22, by + 24, 0.62);
        }
        g.setColor(new Color(40, 30, 30, (int) (255 * alpha)));
        g.drawString(b.text, (float) (bx + 9 + iconW), (float) (by + h / 2 + 4.5));
    }

    // ---------------------------------------------------------------- hover

    /** The customer under the mouse, or null. */
    Customer customerAt(double mx, double my) {
        Customer found = null;
        for (CustomerActor a : customers.values()) {
            if (a.leaving) {
                continue;
            }
            if (mx >= a.x - 22 && mx <= a.x + 22 && my >= a.headTop - 26 && my <= a.y) {
                found = a.customer;
            }
        }
        return found;
    }
}
