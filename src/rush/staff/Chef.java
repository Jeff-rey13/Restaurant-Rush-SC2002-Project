package rush.staff;

import rush.domain.ActionResult;
import rush.domain.Order;
import rush.domain.OrderStatus;

/** The Chef moves one order forward by one preparation unit each turn. */
public class Chef extends Staff {

    public Chef() {
        super("Chef");
    }

    @Override
    public String describeDuties() {
        return "advance one order by one preparation unit";
    }

    public ActionResult prepare(Order order, int turn) {
        if (!isAvailable()) {
            return alreadyBusy();
        }
        if (order.getStatus() == OrderStatus.READY) {
            return ActionResult.fail(order.getLabel() + " is already READY; it needs the Waiter, not the Chef.");
        }
        if (order.getStatus() != OrderStatus.IN_PREPARATION) {
            return ActionResult.fail(order.getLabel() + " is " + order.getStatus() + " and needs no cooking.");
        }
        order.prepareOneUnit(turn);
        markBusy();
        String message = "Chef works on " + order.getLabel() + " (" + order.getItem().getName()
                + "): " + order.getProgress() + ".";
        if (order.getStatus() == OrderStatus.READY) {
            message += " It is now READY (can be served from next turn).";
        }
        return ActionResult.ok(message);
    }
}
