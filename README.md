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

## Current stage: Stage 1 — basic restaurant operation

Regular customers, the three-item menu, Waiter, Chef, Cashier, two tables, the
waiting queue, the full six-step turn sequence, satisfaction, abandonment,
payment, victory and defeat.

## Package layout

| Package | Role |
|---|---|
| `rush.ui` | Boundary: `ConsoleUI` (display + collects choices), `InputReader` (robust input) |
| `rush.control` | Control: `GameController` (turn sequence), `GameConfig`, `ArrivalSchedule`, `GameStats`, `GameResult` |
| `rush.staff` | `Staff` and its roles `Waiter`, `Chef`, `Cashier` |
| `rush.domain` | Entities: `Restaurant`, `Customer`, `Order`, `Table`, `Menu`, `MenuItem`, `PriceCalculator`, `Money`, enums |
