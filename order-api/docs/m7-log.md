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
