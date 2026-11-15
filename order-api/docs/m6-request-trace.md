# M6 request trace: controller to database (2026-11-16 03:07, profiles dev,trace)

Evidence: evidence/m6-p9-trace.txt (full log), evidence/m6-p9-trace-key-lines.txt (key lines).
Order 6afa262de9aaed40829450cf. MongoDB = local mongod 8.3.7 on 127.0.0.1:27018, database "orders".

## Request 1: POST /orders  (thread http-nio-18080-exec-1, every line)

1. Tomcat worker thread http-nio-18080-exec-1 takes the connection and calls DispatcherServlet.
   Evidence: `DispatcherServlet : POST "/orders", parameters={}`
2. Handler mapping picks the controller method from the URL + HTTP method.
   Evidence: `RequestMappingHandlerMapping : Mapped to com.noduco.orders.web.OrderController#create(CreateOrderRequest)`
3. Jackson 3 turns the JSON body into the record.
   Evidence: `RequestResponseBodyMethodProcessor : Read "application/json;charset=UTF-8" to [CreateOrderRequest[customer=Trace, amountPaise=500]]`
4. OrderController.create -> OrderService.create -> new OrderDocument (status NEW, currency INR from OrdersProperties) -> orderRepository.save (proxy $Proxy90, P7).
5. MongoTemplate maps the object to a BSON document (adds _class).
   Evidence: `MongoTemplate : Inserting Document containing fields: [customer, amountPaise, currency, status, createdAt, _class] in collection: orders`
6. The driver sends the command and MongoDB answers.
   Evidence: `Command "insert" started ... to 127.0.0.1:27018 ... "_id": {"$oid": "6afa262de9aaed40829450cf"}` then `Command "insert" succeeded ... in 14.6807 ms ... {"n": 1, "ok": 1.0}`
7. Back in the service: `Created order 6afa262de9aaed40829450cf on Thread[#61,http-nio-18080-exec-1,5,main]`
8. Jackson writes the response record; DispatcherServlet finishes.
   Evidence: `HttpEntityMethodProcessor : Writing [OrderResponse[...]]` then `Completed 201 CREATED`

## Request 2: POST /orders/{id}/actions/PAY  (thread http-nio-18080-exec-2, every line)

1. `DispatcherServlet : POST "/orders/6afa262de9aaed40829450cf/actions/PAY"`
2. `Mapped to com.noduco.orders.web.OrderController#act(String, OrderAction)` ("PAY" converted to OrderAction.PAY)
3. OrderService.apply -> get -> repository.findById:
   `Command "find" ... "filter": {"_id": ...}, "limit": 1` -> succeeded in 1.5773 ms, reply has "status": "NEW"
4. order.apply(PAY) -> OrderStateMachine.next(NEW, PAY) = PAID (pure Java, no log line, no database).
5. repository.save on an existing _id -> replace with upsert:
   `Command "update" ... "q": {"_id": ...}, "u": {... "status": "PAID" ...}, "upsert": true` -> succeeded in 1.537 ms, {"n": 1, "nModified": 1}
6. `Writing [OrderResponse[... status=PAID ...]]` then `Completed 200 OK`

## Responsibilities (who does what)

| Layer | Class | Did in this trace |
|---|---|---|
| Web server | Tomcat (http-nio-18080-exec-N) | one worker thread carried each request from start to end |
| Web framework | DispatcherServlet, RequestMappingHandlerMapping, Jackson converters | URL -> method, JSON <-> records, status code |
| Controller | OrderController | HTTP only: request record in, response record + 201/200 out |
| Service | OrderService | business steps: read, apply rule, write; currency from config |
| Domain | OrderDocument + OrderStateMachine | the NEW -> PAID rule (no I/O) |
| Repository | OrderRepository ($Proxy90) -> MongoTemplate | object <-> BSON, insert/find/update |
| Driver | mongodb-driver-sync 5.8.1 | sends the command over connection ID 3 to 127.0.0.1:27018 |

## Observations from the evidence

- Same thread for every line of one request (exec-1 for create, exec-2 for PAY): thread-per-request, the thread waits while MongoDB answers (M4 tie-in).
- PAY = 2 database round trips (find + update). Nothing stops two PAY/CANCEL requests reading NEW at the same time -> race, see P10/M10.
- save() on an existing id is sent as "update" with "upsert": true, not "insert".
- Extra request in the log: GET "/orders/$id/actions/PAY" (exec-4) -> 405 METHOD_NOT_ALLOWED -> forwarded to /error (BasicErrorController). A literal "$id" URL was opened with GET (not from PowerShell, where $id is expanded); only POST is mapped for that path.

## In my own words (OWED - fill in)

1. What happens between Tomcat and my controller method:
2. Why the business rule is in the domain and not in the controller:
3. What the "update ... upsert: true" line tells me about save():
