package rush.domain;

/**
 * A food critic. Loses 12 satisfaction per waiting turn and pays full price.
 *
 * Always orders Pasta, the house dish, whatever the arrival number.
 *
 * If the food arrives within FAST_SERVICE_TURNS turns of ordering, the critic is
 * impressed (+10 satisfaction). If not, they complain (-10). Pasta takes 2
 * preparation units, so this only works if the Chef starts it on the turn it is
 * ordered, finishes it the next turn, and the Waiter serves it the turn after.
 */
public class CriticCustomer extends Customer {

    public static final String SIGNATURE_DISH = "Pasta";
    private static final int LOSS_PER_TURN = 12;
    private static final int FAST_SERVICE_TURNS = 2;
    private static final int REACTION = 10;

    public CriticCustomer(int number, int arrivalTurn) {
        super(number, arrivalTurn);
    }

    @Override
    public String getTypeName() {
        return "Critic";
    }

    @Override
    public int getSatisfactionLossPerTurn() {
        return LOSS_PER_TURN;
    }

    @Override
    public String describeBehaviour() {
        return super.describeBehaviour() + ", always orders " + SIGNATURE_DISH + ", +" + REACTION
                + " if served within " + FAST_SERVICE_TURNS + " turns of ordering, otherwise -" + REACTION;
    }

    @Override
    public MenuItem chooseMenuItem(Menu menu) {
        return menu.findByName(SIGNATURE_DISH);
    }

    @Override
    protected String reactToService(int turnsSinceOrder) {
        if (turnsSinceOrder <= FAST_SERVICE_TURNS) {
            adjustSatisfaction(REACTION);
            return getLabel() + " (Critic) is impressed by the fast service: +" + REACTION
                    + " satisfaction (now " + getSatisfaction() + ").";
        }
        adjustSatisfaction(-REACTION);
        return getLabel() + " (Critic) complains the food took " + turnsSinceOrder + " turns: -"
                + REACTION + " satisfaction (now " + getSatisfaction() + ").";
    }
}
