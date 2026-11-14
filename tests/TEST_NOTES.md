# Test notes

Each stage's scripts must be run on that stage's tagged code
(`git checkout stage-1`, `stage-2` or `stage-3`), because later stages change the
customer mix and the menu.

All runs are fully deterministic (no randomness), so anyone can reproduce them.
Each script is plain text: one answer per line, lines starting with `#` are comments
explaining the manager's decision.

```sh
./run.sh --echo < tests/stage1-victory.txt      # Linux / macOS
run.bat --echo < tests\stage1-victory.txt       # Windows
```

`--echo` prints each answer so the output reads like a live session.
Every script can also be watched in the game window, which reads the answers exactly
as the console does and ends with the same result screen:

```sh
java -cp out rush.gui.GuiMain --replay tests/stage3-victory.txt --speed 2
```
The saved output of every run is in `tests/transcripts/`.
The rule tests (`./run-tests.sh`) check individual rules without the UI.

## Stage 1

| Script | Expected result | Key evidence |
|---|---|---|
| `stage1-victory.txt` | **VICTORY**: 5 paid, $66.00, avg satisfaction 79.2 | Full attention to each customer. Turn 9 attempts to serve a half-cooked Pasta: rejected, task not used up. C3 follows exactly the brief's §3.3 timeline (92 → 84 → 76 → served → READY_TO_PAY → pays 76). C6 (turn 16) cannot pay in time, as intended. |
| `stage1-defeat.txt` | **DEFEAT**: 0 paid, $0.00, N/A | Neglectful manager seats C1 and C2 but never takes orders. C1 leaves at end of turn 13, C2 at end of turn 16 (8 per turn from 100). Game still runs all 18 turns. |
| `stage1-invalid-input.txt` | stops after turn 3 (input ends) | Non-numbers, out-of-range numbers, impossible tasks (seat with empty queue, order from an unseated customer, re-ordering) are all explained and re-asked without using up the staff task. |

Customer food choices in Stage 1 (rule: customer n orders menu item ((n − 1) mod 3) + 1):
C1 Salad, C2 Burger, C3 Pasta, C4 Salad, C5 Burger, C6 Pasta.

## Stage 2

Arrivals: C1 Regular (turn 1), **C2 Critic** (turn 4), **C3 VIP** (turn 7), C4, C5, C6 Regular (turns 10, 13, 16).
Food: C1 Salad, C2 Pasta (Critic always orders Pasta), C3 Pasta, C4 Salad, C5 Burger, C6 Pasta.

| Script | Expected result | Key evidence |
|---|---|---|
| `stage2-victory.txt` | **VICTORY**: 5 paid, $67.20, avg satisfaction 78.0 | Turn 7 is a constrained choice: the Waiter can seat the arriving VIP or serve the Critic. Serving the Critic exactly 2 turns after ordering triggers "impressed" (+10, 64 → 74). The VIP's Pasta is billed $16.20 (10% off) and the VIP loses only 5 per turn while waiting. |
| `stage2-defeat.txt` | **DEFEAT**: 3 paid, $43.20, avg 64.7 | "One customer at a time" manager never uses the second table. Shows that the same rules with a worse seating policy miss the paid-customer and revenue targets. |

The Critic's "complains" reaction (served more than 2 turns after ordering, −10)
is checked in the rule tests (`criticReactsToServiceSpeed`).

## Stage 3

Same arrivals as Stage 2. The menu now has five items, so the cycling rule gives:
C1 Salad, C2 Pasta (Critic), C3 Pasta, **C4 Salad + Burger Combo** ($21.60, 2 units),
**C5 Salad + Pasta Combo** ($24.30, 3 units), C6 Salad.
Each turn asks, in order: Happy Hour (only until used), Host, Waiter, Chef.

| Script | Expected result | Key evidence |
|---|---|---|
| `stage3-victory.txt` | **VICTORY**: 5 paid, $85.86, avg satisfaction 86.6 | Host seats every arrival on its arrival turn, so the Waiter takes the order the same turn. Happy Hour switched on in turn 7: VIP Pasta billed $18 × 0.9 × 0.8 = **$12.96**; display shows the remaining duration; on turn 8 the waiting VIP recovers 95 → 100. Both combos are cooked unit by unit (2 and 3 units) and paid at full combo price after Happy Hour ended. Critic impressed (+10). |
| `stage3-defeat.txt` | **DEFEAT**: 3 paid, $43.20, avg 64.7 | Manager never uses the Host, Happy Hour or the second table. Same customers as the victory run; only the choices differ. |
| `stage3-invalid-input.txt` | stops after turn 7 | Invalid input at the Happy Hour and Host prompts; Host and Waiter both refused when no table is free (task kept); Happy Hour recovery for seated customers (52 → 67, 64 → 79; capped at 100); Regular Salad in Happy Hour billed $7.20. |

## Rule tests (`./run-tests.sh`)

65 checks, all passing at `stage-3`, including the brief's §3.3 example turn by turn,
money rounding ($24.30 combo, $10.80 VIP Happy Hour burger), invalid choices not using
up a task, dish READY this turn not servable this turn, abandonment cancelling the
order and freeing the table, VIP/Critic behaviour, Critic fast/slow reaction, Happy
Hour once-only / two-turn duration / price lock / recovery cap, Host + Waiter seating
in the same turn.
