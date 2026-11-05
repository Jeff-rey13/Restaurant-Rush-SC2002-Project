package rush.staff;

import rush.domain.ActionResult;

/**
 * A staff member who can do at most one task per turn.
 * Subclasses add the tasks for their role.
 */
public abstract class Staff {

    private final String role;
    private boolean busyThisTurn;

    protected Staff(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    /** Short description of what this role can do in a turn (shown to the manager). */
    public abstract String describeDuties();

    public boolean isAvailable() {
        return !busyThisTurn;
    }

    /** Called by the controller at the start of each turn. */
    public void startTurn() {
        busyThisTurn = false;
    }

    /** Uses up this turn's task doing nothing. */
    public ActionResult waitThisTurn() {
        if (!isAvailable()) {
            return alreadyBusy();
        }
        markBusy();
        return ActionResult.ok(role + " waits this turn.");
    }

    protected void markBusy() {
        busyThisTurn = true;
    }

    protected ActionResult alreadyBusy() {
        return ActionResult.fail("The " + role + " has already completed a task this turn.");
    }
}
