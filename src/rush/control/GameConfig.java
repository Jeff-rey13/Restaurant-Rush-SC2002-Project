package rush.control;

/** The base game settings from Section 2.1 of the brief. Cannot be changed. */
public final class GameConfig {

    private final int totalTurns;
    private final int tableCount;
    private final int targetPaidCustomers;
    private final int targetRevenueCents;
    private final int targetAverageSatisfaction;

    public GameConfig(int totalTurns, int tableCount, int targetPaidCustomers,
                      int targetRevenueCents, int targetAverageSatisfaction) {
        this.totalTurns = totalTurns;
        this.tableCount = tableCount;
        this.targetPaidCustomers = targetPaidCustomers;
        this.targetRevenueCents = targetRevenueCents;
        this.targetAverageSatisfaction = targetAverageSatisfaction;
    }

    /** 18 turns and 2 tables. To win: at least 4 paid, at least $45 revenue, average satisfaction of at least 60. */
    public static GameConfig base() {
        return new GameConfig(18, 2, 4, 4500, 60);
    }

    public int getTotalTurns() {
        return totalTurns;
    }

    public int getTableCount() {
        return tableCount;
    }

    public int getTargetPaidCustomers() {
        return targetPaidCustomers;
    }

    public int getTargetRevenueCents() {
        return targetRevenueCents;
    }

    public int getTargetAverageSatisfaction() {
        return targetAverageSatisfaction;
    }
}
