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
- P2: OrderStateMachineTests: 4 allowed + 8 refused (@ParameterizedTest @CsvSource) + coverage check over all 12 status x action combinations; 13 tests in 0.213 s, no Spring/DB. Broke CANCEL -> PAID on purpose: 2 failures (expected CANCELLED but was PAID); restored with git restore. Evidence: m8-p2-broken-rule.txt.
- P3: OrderServiceTests (Mockito, no Spring/DB, 7 tests in 1.3 s): no key, new key (ArgumentCaptor checks fingerprint Asha|500 and orderId), replay, in progress (pred 4: b IdempotencyInProgressException), key reused, failed create releases key, stale If-Match. Removed deleteById on purpose -> "Wanted but not invoked: idempotency.deleteById(k1)"; restored. Evidence: m8-p3-missing-cleanup.txt.
- P4: OrderControllerWebTests (@WebMvcTest + MockMvc + @MockitoBean OrderService; Boot 4 packages work): validation 400 + 2 errors and service untouched, 404 problem + correlationId, 201 Location + ETag "0", replay header, 412 version-mismatch, size=500 400 naming size, correlation id echo. 7 tests in ~4 s, no MongoDB driver started. Without @MockitoBean: context fails, UnsatisfiedDependencyException No qualifying bean of type OrderService (pred 3: b). Evidence: m8-p4-no-mock.txt.
- P5: rename amountPaise -> amount with existing tests: 31 run, 0 failures, BUILD SUCCESS (pred 5: b, none). Added OrderResponseContractTests (6 v1 fields pinned; extra request field giftWrap accepted -> tolerant reader pinned) and OpenApiCompatibilityTests (9 baseline paths/params from docs/openapi.yaml must exist in live /v3/api-docs.yaml; missing []). Same rename now fails: No value at JSON path $.amountPaise. Evidence: m8-p5-rename-before/after.txt.
- P6: spring-boot-testcontainers + testcontainers-junit-jupiter + testcontainers-mongodb (test, Boot BOM -> Testcontainers 2.0.5). Docker Desktop 29.1.3 already installed, so the test was NOT skipped (pred 6: none of the options). Docker Hub pull of mongo:8.0 failed with EOF from production.cloudfront.docker.com (same as M6 P0; small ryuk image pulled fine) -> test errored (ContainerFetchException), build red. Fix: Google mirror mirror.gcr.io/library/mongo:8.0 pulled OK (323 MB); test uses DockerImageName.parse(mirror).asCompatibleSubstituteFor("mongo"). MongoBehaviourContainerTests: container started in 1.1 s, duplicate _id -> DuplicateKeyException, stale save -> OptimisticLockingFailureException, 2 tests green. NOTE for M11: use mirror.gcr.io for Docker Hub images on this network. Evidence: m8-p6-testcontainers.txt.
