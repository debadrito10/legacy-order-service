# M3 log - modern Java in legacy-order-service (JDK 25)

Branch: dev. Starting point: the P0 commit (M2 complete, 6/6 tests on JDK 25).
Evidence files live in migration\ (m3-p<N>-*.txt), raw output from Save-Evidence, never edited.
Mode: built first in execution mode (2026-11-05); study and exit check follow after P10.

| Step | Feature (JDK) | What changed | Tests after | Commit |
|---|---|---|---|---|
| P0 | - | Baseline evidence on JDK 25; M2 log committed | 6 | (this commit) |

### P0 - Baseline
- Goal: a known-good starting point to compare every M3 change against.
- Change: no code. M2 log committed (Expected lines; R5 stray-edit note corrected).
- Expected: 6/6 tests, BUILD SUCCESS, app prints 1-5 and Done., SMOKE PASS.
- Result: (fill in from the evidence files)
- Evidence: m3-p0-build-java25.txt, m3-p0-app-java25.txt, m3-p0-smoke-java25.txt
- Why (for study): without a baseline you cannot tell whether a later difference came from your change or was already there.