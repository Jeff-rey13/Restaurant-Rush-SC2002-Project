# Test notes

All runs are fully deterministic (no randomness), so anyone can reproduce them.
Each script is plain text: one answer per line, lines starting with `#` are comments
explaining the manager's decision.

```sh
./run.sh --echo < tests/stage1-victory.txt      # Linux / macOS
run.bat --echo < tests\stage1-victory.txt       # Windows
```

`--echo` prints each answer so the output reads like a live session.
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
