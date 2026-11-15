# M6 log - Spring Boot foundations and application architecture

Branch: m6-spring-boot, from f992b7e (jdk25-migration). PR #2 checked 2026-11-15: ready, OPEN, MERGEABLE.
MongoDB: mongo:8.0 Docker image failed to download (same layer 850d4854..., EOF from production.cloudfront.docker.com, twice).
Fallback: second local mongod 8.3.7 (C:\Program Files\MongoDB\Server\8.3\bin\mongod.exe) on 127.0.0.1:27018,
data C:\java-practice\m6-mongo-data, started with Start-Process (log file mongod.log in the data folder).
Local MongoDB service (8.3) already listens on 127.0.0.1:27017. mongosh is not installed.
Spring Boot: 4.1.2 (published 2026-10-22, 24 days old; newest final release; 4.2.0 not final; 4.1 supported longer than 4.0).

## Predictions (written before P1)
| # | Question | My prediction |
|---|---|---|
| 1 | P1: does the generated test pass with MongoDB stopped? | |
| 2 | P3: does the app start with MongoDB stopped? What does /actuator/health say? | |
| 3 | P5: status code of POST /orders, and how createdAt looks in the JSON | |
| 4 | P6: env var NODUCO_ORDERS_CURRENCY=USD and --noduco.orders.currency=EUR both set: which wins? | |
| 5 | P7: order of constructor, @PostConstruct, ApplicationReadyEvent, @PreDestroy | |
| 6 | P7: two Clock beans and one constructor asking for Clock: what happens and when? | |
| 7 | P10: @Transactional on a service method, standalone MongoDB, no transaction manager: what happens? | |

## Steps
| Step | Result | Commit |
|---|---|---|
