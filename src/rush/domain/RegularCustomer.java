package rush.domain;

/** A regular customer: loses 8 satisfaction per waiting turn and pays full price. */
public class RegularCustomer extends Customer {

    private static final int LOSS_PER_TURN = 8;

    public RegularCustomer(int number, int arrivalTurn) {
        super(number, arrivalTurn);
    }

    @Override
    public String getTypeName() {
        return "Regular";
    }

    @Override
    public int getSatisfactionLossPerTurn() {
        return LOSS_PER_TURN;
    }
}
