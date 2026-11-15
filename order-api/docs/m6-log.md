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
| 1 | P1: does the generated test pass with MongoDB stopped? | b) fails, the app needs the database to start |
| 2 | P3: does the app start with MongoDB stopped? What does /actuator/health say? | b) starts, but health says DOWN |
| 3 | P5: status code of POST /orders, and how createdAt looks in the JSON | c) 204 No Content |
| 4 | P6: env var NODUCO_ORDERS_CURRENCY=USD and --noduco.orders.currency=EUR both set: which wins? | not sure |
| 5 | P7: order of constructor, @PostConstruct, ApplicationReadyEvent, @PreDestroy | a) constructor, @PostConstruct, app ready, @PreDestroy |
| 6 | P7: two Clock beans and one constructor asking for Clock: what happens and when? | b) Spring picks one quietly and the app starts |
| 7 | P10: @Transactional on a service method, standalone MongoDB, no transaction manager: what happens? | not sure |

## Steps
| Step | Result | Commit |
|---|---|---|

## Results
| # | Prediction | Actual | Evidence |
|---|---|---|---|
| 1 | b) fails without DB | WRONG: passes (1 test, BUILD SUCCESS) with nothing on 27019; driver monitor thread logged Connection refused in background, no query was sent | evidence/m6-p1-build-nomongo.txt |

## Notes
- P1: SPRING_MONGODB_URI env var -> hosts=[127.0.0.1:27019], so Boot 4 reads spring.mongodb.uri (not spring.data.mongodb.uri).
- P1: Mockito self-attaching warning (same as M5). Deferred to M8.
- P2: effective pom: java.version 25 -> maven.compiler.release 25 (property, line 161); compiler plugin 3.15.0 chosen by Boot BOM; parameters=true.
- P2: tree: autoconfigure 4.1.2, mongodb-driver-sync 5.8.1, spring-tx 7.0.10, jackson-databind 3.1.7 (tools.jackson), tomcat-embed-core 11.0.27 = compile; junit-jupiter-api 6.0.3, mockito-core 5.23.0 = test. No versions in my pom: all from the parent BOM.
- P2: OWED in my own words: (1) where starter versions come from, (2) why JUnit is test scope, (3) why spring-tx is present.
- P3: no URI configured -> driver default hosts=[localhost:27017] = the MongoDB SERVICE, health UP (200). Silent wrong-database trap: nothing failed.
- P3: spring.mongodb.uri in application.yaml -> hosts=[127.0.0.1:27018], connected to the M6 mongod.
- P3: conditions report: MongoAutoConfiguration + DataMongoAutoConfiguration matched (MongoClient, MongoTemplate made by Boot); 0 repositories; Jackson2 negative (Boot 4 = Jackson 3); DataSourceAutoConfiguration not present (no JDBC module); EnableTransactionManagementConfiguration did NOT match (no TransactionManager bean).
- P3 lesson: a --arg typed on the same line as the next command became part of the file name (m6-p3-debug.txtClear-Host).
| 2 | b) starts, health DOWN | PARTLY CONFIRMED: app started with nothing on 27019; health took 30.2 s (MongoHealthIndicator 30004 ms = serverSelectionTimeout 30000 ms) HTTP code not captured (output not pasted); re-check in P6 | console only, not saved |
- P4: OrderStatus, OrderAction, OrderStateMachine copied from dev (M3) with git show, only the package line changed. OrderDocument (@Document orders, @Id, money as long paise) and OrderRepository (interface only) compile.
| 3 | c) 204 No Content | WRONG: 201 Created + Location /orders/<ObjectId>; createdAt ISO-8601 Z (Jackson 3); POST 17:40:31.624539100Z vs GET .624Z = Mongo stores milliseconds | evidence/m6-p5-calls.txt |
- P5: GET 200 NEW, PAY 200 PAID, PAY again 409 (state machine), unknown id 404 (OrderNotFoundException), REFUND 400 (Spring rejected before my code; different error JSON shape -> fix in M7).
- P5: starting a 2nd copy of the app -> "Port 18080 was already in use", APPLICATION FAILED TO START (fail fast).
| 4 | not sure | RUN 3 env NODUCO_ORDERS_CURRENCY=USD over yaml INR -> USD. RUN 4 env USD + --noduco.orders.currency=EUR -> {"id":"6afa12f7ef43106c6823e4d1","customer":"Asha","amountPaise":100,"currency":"EUR","status":"NEW","createdAt":"2026-11-15T20:15:51.257303500Z"} | evidence/m6-p6-profiles.txt |
- P6: no profile: /actuator/beans 404, 1 endpoint exposed. --spring.profiles.active=dev: 200, 5 endpoints. Request thread = http-nio-18080-exec-3 (Tomcat platform thread).
| 5 | a) constructor, @PostConstruct, app ready, @PreDestroy | RIGHT: constructor -> @PostConstruct -> Tomcat started -> Started -> ApplicationReadyEvent; Ctrl+C -> graceful shutdown of Tomcat FIRST, then @PreDestroy (thread SpringApplicationShutdownHook). Repository = jdk.proxy2.$Proxy90 (implements OrderRepository, TransactionalProxy); service = plain OrderService | evidence/m6-p7-lifecycle.txt |
| 6 | b) Spring picks one quietly | HALF RIGHT: clock + indiaClock -> started (parameter name "clock" matched bean name; needs parameters=true from P2). utcClock + indiaClock -> APPLICATION FAILED TO START at startup: "Parameter 1 of constructor in OrderService required a single bean, but 2 were found"; fix = @Primary or @Qualifier. Restored ClockConfig. | evidence/m6-p7-two-clocks.txt |
- P8: /actuator/beans (dev): orderController <- orderService <- (orderRepository, clock, OrdersProperties); orderRepository <- mongoTemplate <- mongoDatabaseFactory <- mongo (MongoClientImpl). All singleton. No TransactionManager bean. clockConfig is $$SpringCGLIB$$0. 3 handlers mapped. See m6-bean-wiring.md.
- P9: trace (dev,trace): create = 1 thread (exec-1): DispatcherServlet -> OrderController#create -> Jackson -> MongoTemplate insert -> driver insert 14.7 ms -> 201. PAY (exec-2): find 1.6 ms -> state machine -> update upsert 1.5 ms -> 200. Literal GET /orders/$id/actions/PAY -> 405. See m6-request-trace.md.
- P10a: @Transactional on OrderService.apply, NO TransactionManager bean: service class stays plain OrderService, no transaction lines at TRACE, PAY -> HTTP 200 PAID. Annotation silently ignored. (pred 7)
- P10b: + MongoTxConfig (MongoTransactionManager bean): service class becomes OrderService$$SpringCGLIB$$0; "Creating new transaction ... OrderService.apply PROPAGATION_REQUIRED"; find fails: error 20 IllegalOperation "Transaction numbers are only allowed on a replica set member or mongos" (mongod 27018 is STANDALONE); "Initiating transaction rollback"; PAY -> HTTP 500. Reverted both changes: standalone mongod cannot run transactions; real fix needs a replica set (M10).
- Pred 7 answered: @Transactional needs a TransactionManager bean (else silently ignored, no proxy) AND a database that supports transactions (Mongo: replica set).
- P11: OrderFlowTests (RANDOM_PORT, real HTTP via java.net.http, db orders-test on 27018): 201 -> PAY 200 PAID (checked in Mongo) -> PAY again 409 -> unknown id 404. clean verify: Tests run: 2, Failures: 0, BUILD SUCCESS. Not yet seen failing (owed).
