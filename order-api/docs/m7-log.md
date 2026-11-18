# M7 log: REST contract and reliability

Branch m7-rest-contract (from m6-spring-boot 461191e). Guide: https://claude.ai/artifact/Kdi4md7rSa7GvP1b5Zd45f
Database for M7 manual runs: orders-m7 on 127.0.0.1:27018 (fresh, so every order has a version field from P3). Tests: orders-test.

## Predictions (2026-11-16, before any code)

| # | Question | Answer |
| --- | --- | --- |
| 1 | constraints but no @Valid: POST customer "" amount -5 | a) 400 |
| 2 | @Valid only: shape of the 400 body | a) {timestamp,status,error,path} |
| 3 | naive find-then-save idempotency, 10 threads same key | not sure |
| 4 | two PAYs at once on a NEW order, no version | not sure |
| 5 | GET with If-None-Match = current ETag | not answered (not sure) |
| 6 | MDC correlation id on driver log lines | not answered (not sure) |

## Steps
- P0: branch m7-rest-contract from m6-spring-boot; manual runs use db orders-m7 (old-db order -> 404 proves new jar); helper m7-api.ps1 (curl via files); smoke POST 201 + GET 200. Lesson: start Window A only after the build prompt returns (jar locked before port 18080 opens).
- P1: validation starter + @NotBlank/@Size/@Positive/@Max on CreateOrderRequest. Without @Valid: 201, order SAVED with customer "" and amount -5 (pred 1 WRONG: annotations alone do nothing). With @Valid: 400, 400, 201 (pred 2 RIGHT: default {timestamp,status,error,path}, application/json, no field names). Evidence: evidence/m7-p1-validation.txt. (@Valid line itself committed with P2, controller rewritten there.)
- P2: ApiExceptionHandler extends ResponseEntityExceptionHandler; controller handlers removed; @Valid on create. All errors application/problem+json: 400 validation (+errors per field), 404 order-not-found, 409 invalid-transition; Spring own: 400 REFUND, 405, 404 unknown URL (type omitted = about:blank). Tests run: 2 green. Evidence: evidence/m7-p2-problems.txt.
- P3: OrderConcurrencyTests 20x PAY at once. Before @Version: 20/20 got 200 in 3/3 runs, build RED (pred 4: b, all can win). @Version Long on OrderDocument = conditional save: exactly 1x200 + 19x409 in 3/3 runs, Tests run: 3 green. 409 kinds: invalid-transition (read after winner saved) vs concurrent-modification (read before, save lost the version check). ETag = version, Cache-Control no-cache. If-None-Match "0" -> 304 (pred 5: b). If-Match stale "0" -> 412, current "1" -> 200. Evidence: m7-p3-race-before/after.txt, m7-p3-etag.txt.
- P4: GET /orders?page&size, default 20, max 100, sort createdAt desc + id (tiebreaker). Pages 0 and 1 of size 2: no overlap, totalElements 6, totalPages 3. Bad size/page first gave only "Validation failure" (HandlerMethodValidationException, no field names); added handleHandlerMethodValidationException -> same validation type + errors[{field,message}] as body errors. Tests run: 3 green. Evidence: m7-p4-paging.txt, m7-p4-paging-errors.txt.
- P5: CorrelationIdFilter (HIGHEST_PRECEDENCE; X-Correlation-Id in/out; allow-list [A-Za-z0-9-]{1,64} else UUID; MDC put + finally remove), logging.pattern.level %5p [%X{correlationId:-}], correlationId in every Problem Details. demo-123 on all 10 log lines of the create incl. MongoTemplate and driver insert started/succeeded, same thread exec-1 (pred 6: a). No header -> UUID returned; unsafe value -> replaced. Tests run: 3 green. Evidence: m7-p5-responses.txt, m7-p5-demo-123-lines.txt.
