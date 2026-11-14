package rush.control;

/** Running totals for the end-of-turn report and the final result. */
public class GameStats {

    private int revenueCents;
    private int paidCustomers;
    private int paidSatisfactionTotal;
    private int servedCount;
    private int unhappyDepartures;

    public void recordPayment(int amountCents, int satisfactionAtPayment) {
        revenueCents += amountCents;
        paidCustomers++;
        paidSatisfactionTotal += satisfactionAtPayment;
    }

    public void recordServed() {
        servedCount++;
    }

    public void recordUnhappyDeparture() {
        unhappyDepartures++;
    }

    public int getRevenueCents() {
        return revenueCents;
    }

    public int getPaidCustomers() {
        return paidCustomers;
    }

    public int getServedCount() {
        return servedCount;
    }

    public int getUnhappyDepartures() {
        return unhappyDepartures;
    }

    public boolean hasPaidCustomers() {
        return paidCustomers > 0;
    }

    /** Average satisfaction at payment. Only makes sense once someone has paid. */
    public double getAverageSatisfaction() {
        return paidCustomers == 0 ? 0.0 : (double) paidSatisfactionTotal / paidCustomers;
    }

    public boolean meetsPaidTarget(GameConfig config) {
        return paidCustomers >= config.getTargetPaidCustomers();
    }

    public boolean meetsRevenueTarget(GameConfig config) {
        return revenueCents >= config.getTargetRevenueCents();
    }

    /** Uses whole numbers (total >= target * count) to avoid rounding errors. */
    public boolean meetsSatisfactionTarget(GameConfig config) {
        return paidCustomers > 0
                && paidSatisfactionTotal >= config.getTargetAverageSatisfaction() * paidCustomers;
    }

    public GameResult evaluate(GameConfig config) {
        boolean won = meetsPaidTarget(config) && meetsRevenueTarget(config)
                && meetsSatisfactionTarget(config);
        return won ? GameResult.VICTORY : GameResult.DEFEAT;
    }
}
