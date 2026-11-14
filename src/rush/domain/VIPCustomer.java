package rush.domain;

/** A VIP customer: more patient (loses 5 per waiting turn) and gets 10% off the bill. */
public class VIPCustomer extends Customer {

    private static final int LOSS_PER_TURN = 5;
    private static final int PERCENT_PAYABLE = 90;

    public VIPCustomer(int number, int arrivalTurn) {
        super(number, arrivalTurn);
    }

    @Override
    public String getTypeName() {
        return "VIP";
    }

    @Override
    public int getSatisfactionLossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String describeBehaviour() {
        return super.describeBehaviour() + ", pays 10% less";
    }

    @Override
    public int adjustBill(int fullPriceCents) {
        return Money.percentOf(fullPriceCents, PERCENT_PAYABLE);
    }
}
