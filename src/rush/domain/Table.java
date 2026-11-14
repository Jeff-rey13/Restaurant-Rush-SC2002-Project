package rush.domain;

/** A table that holds at most one customer until that customer pays or leaves. */
public class Table {

    private final int number;
    private Customer occupant;

    public Table(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public String getLabel() {
        return "Table " + number;
    }

    public boolean isFree() {
        return occupant == null;
    }

    public Customer getOccupant() {
        return occupant;
    }

    void assign(Customer customer) {
        if (!isFree()) {
            throw new IllegalStateException(getLabel() + " is already occupied");
        }
        occupant = customer;
    }

    public void release() {
        occupant = null;
    }
}
