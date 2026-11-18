# M8 log: Git workflow and test strategy

Branch m8-test-strategy (from m7-rest-contract ad1ab05). Guide: https://claude.ai/artifact/1AJFEF4bgQoStFuehThjVe

## Predictions (2026-11-18, before any work)

| # | Question | Answer |
| --- | --- | --- |
| 1 | rebase onto a branch that changed the same line | a) Git keeps the newer line |
| 2 | git revert: what happens to the original commit | not answered |
| 3 | @WebMvcTest without @MockitoBean OrderService | not answered |
| 4 | Mockito: duplicate key + record with orderId null | not answered |
| 5 | rename amountPaise: how many existing tests fail | not answered |
| 6 | Testcontainers test without Docker | not answered |

## Steps
- P1: git lab (C:\java-practice\git-lab + bare git-lab-origin.git + git-lab-colleague clone). fetch: file unchanged, status [behind 1], log main..origin/main showed the incoming commit; pull applied it. Merge conflict (10 vs 15): markers, resolved to 12 by hand, git add = resolved, merge commit 4a369db with 2 parents. Rebase conflict (20 vs 25): rebase STOPPED, Git kept neither line (pred 1 a WRONG); during rebase HEAD = the other side (25), mine shown as incoming; resolved 22, commit id 860dfd7 -> eb8af1b, linear history. Revert: original eb8af1b kept, new commit b5cb58f undoes it, file back to 25 (pred 2: b). Evidence: m8-p1-git-lab.txt.
