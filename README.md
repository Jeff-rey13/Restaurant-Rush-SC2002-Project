# Restaurant Rush (SC2002 group assignment)

A turn-based restaurant management game for the command line. You are the manager:
each turn you give the Waiter and the Chef one task each, and after 18 turns the
restaurant must reach the targets to win.

## Build and run

Requires Java 11 or newer (JDK). No external libraries and no network access needed.

```sh
./run.sh                 # Linux / macOS: compiles into out/ and starts the game
run.bat                  # Windows
./run-tests.sh           # compiles and runs the rule tests
```

Manual equivalent:

```sh
javac -d out $(find src -name "*.java")
java -cp out rush.Main
```

## Game window (2D)

The same game also runs in a drawn, animated window: an isometric restaurant with a
waiter, chef, cashier and customers who walk in, sit, order, eat and pay. It uses
exactly the same `GameController`, so every rule and result is identical to the
command line.

```sh
./run-gui.sh                                   # Linux / macOS
run-gui.bat                                    # Windows
java -cp out rush.gui.GuiMain                  # after compiling
java -cp out rush.gui.GuiMain --replay tests/stage1-victory.txt --speed 2   # watch a test script
```

- Click a choice (or press 1-9, 0 to wait, Enter for the next turn).
- The yellow **Manager's tip** suggests a sensible move and marks it with a TIP badge (T turns tips off).
- **Help** (H) explains the rules; hover over a customer for their mood, order and progress.
- A refused choice shows the reason and does not use up the task.
- S toggles sound effects, M the background music (both are generated in code; silent if there is no sound device).

Scripted, reproducible runs are described in [tests/TEST_NOTES.md](tests/TEST_NOTES.md).

## Stages

| Tag | Adds |
|---|---|
| `stage-1` | Regular customers, menu, Waiter, Chef, Cashier, tables, waiting queue, full turn sequence, victory and defeat |
| `stage-2` | VIP and Critic customers |

### Customer types (Stage 2)

| Type | Loss per waiting turn | Bill | Special behaviour |
|---|---|---|---|
| Regular | 8 | full price | — |
| VIP | 5 | 10% off | — |
| Critic | 12 | full price | Always orders the signature **Pasta**. Reacts to service speed: served within 2 turns of ordering → impressed, +10 satisfaction; slower → complains, −10. |

Food choice rule (Regular and VIP): customer number *n* orders menu item ((n − 1) mod menu size) + 1.

## Stage 1 → Stage 2 changes

**New files**
- `domain/VIPCustomer.java` — overrides loss per turn (5) and `adjustBill` (10% off).
- `domain/CriticCustomer.java` — overrides loss per turn (12), `chooseMenuItem` (Pasta) and `reactToService` (±10).

**Changed files**
- `domain/Customer.java` — added three overridable behaviour points with neutral defaults: `adjustBill` (full price), `reactToService` (no reaction, called from the now-`final` `receiveFood`) and `describeBehaviour`; plus a protected `adjustSatisfaction` so subclasses can change satisfaction without breaking the 0–100 limit. Reason: the new types need places to plug in their behaviour; the defaults keep Regular customers behaving exactly as in Stage 1.
- `domain/PriceCalculator.java` — one line: asks the customer for its bill (`customer.adjustBill(...)`) instead of using the raw price. Reason: VIP discount dispatched polymorphically, no type checks.
- `staff/Waiter.java` — `serve` appends the customer's reaction message. Reason: the Critic's reaction must be visible to the manager.
- `control/ArrivalSchedule.java` — turns 4 and 7 now create a Critic and a VIP (registration change only).
- `control/GameController.java` — arrival message also shows `describeBehaviour()` (display text only).
- `test/rush/RuleTests.java` — §3.3 example now uses its own one-customer setup; new VIP/Critic checks.

**Unchanged**: turn sequence, `Order`, `Table`, `Restaurant`, `Chef`, `Cashier`, `ConsoleUI`. The controller contains no customer-type checks.

**Game window (Stage 2 changes)**
- `gui/Appearances.java` — registers a style (look, greeting, name badge) for `"VIP"` and `"Critic"`; the drawing code looks the style up by `getTypeName()`, so there is no type check in the window either.
- `gui/Look.java`, `gui/CharacterPainter.java` — two new outfits: VIP suit with sunglasses, Critic beret with glasses and notebook.
- `gui/World.java` — when a customer's satisfaction jumps at the moment of serving (the Critic's reaction), a speech bubble shows it.
- `gui/Advisor.java` — tips now rank customers by how many turns they could still wait (satisfaction / loss per turn), because types lose patience at different rates.
- `gui/GamePanel.java` — hover card shows `describeBehaviour()`.

Compare: `git diff stage-1 stage-2` (on GitHub: `/compare/stage-1...stage-2`).

## Package layout

| Package | Role |
|---|---|
| `rush.ui` | Boundary: `ConsoleUI` (display + collects choices), `InputReader` (robust input) |
| `rush.control` | Control: `GameController` (turn sequence), `GameConfig`, `ArrivalSchedule`, `GameStats`, `GameResult` |
| `rush.staff` | `Staff` and its roles `Waiter`, `Chef`, `Cashier` |
| `rush.domain` | Entities: `Restaurant`, `Customer` (+ `RegularCustomer`, `VIPCustomer`, `CriticCustomer`), `Order`, `Table`, `Menu`, `MenuItem`, `PriceCalculator`, `Money`, enums |
