# M6 bean wiring (from /actuator/beans and /actuator/mappings, dev profile, 2026-11-16 03:01)

Evidence: evidence/m6-p8-beans.txt. All beans below are scope singleton (one shared instance for the whole app).

## Request chain, top to bottom

| Bean | Type | Made by | Injected with (from "dependencies") |
|---|---|---|---|
| orderController | com.noduco.orders.web.OrderController | me (@RestController, found by component scan) | orderService |
| orderService | com.noduco.orders.service.OrderService | me (@Service) | orderRepository, clock, noduco.orders-...OrdersProperties |
| orderRepository | declared type OrderRepository; runtime class jdk.proxy2.$Proxy90 (P7) | Spring Data (DataMongoRepositoriesAutoConfiguration) | mongoTemplate, mongo.named-queries#0, mongodb.OrderRepository.fragments#0 |
| mongoTemplate | org.springframework.data.mongodb.core.MongoTemplate | Boot auto-config (MongoDatabaseFactoryDependentConfiguration) | mongoDatabaseFactory, mappingMongoConverter |
| mongoDatabaseFactory | SimpleMongoClientDatabaseFactory | Boot auto-config (MongoDatabaseFactoryConfiguration) | mongo, MongoProperties, mongoConnectionDetails |
| mongo | com.mongodb.client.internal.MongoClientImpl (the MongoClient) | Boot auto-config (MongoAutoConfiguration) | mongoClientSettings |
| mongoConnectionDetails | PropertiesMongoConnectionDetails | Boot auto-config (MongoAutoConfiguration) | MongoProperties (= spring.mongodb.uri from application.yaml) |

## Side beans

| Bean | Type | Made by | Injected with |
|---|---|---|---|
| clock | java.time.Clock$SystemClock | me (@Bean method in ClockConfig) | clockConfig |
| clockConfig | ClockConfig$$SpringCGLIB$$0 (a subclass Spring generated) | me (@Configuration) | - |
| noduco.orders-...OrdersProperties | com.noduco.orders.config.OrdersProperties | me (@ConfigurationProperties + @ConfigurationPropertiesScan), filled by Boot's binder | configuration properties binder |
| lifecycleProbe | com.noduco.orders.config.LifecycleProbe | me (@Component) | orderRepository, orderService |
| mappingMongoConverter | MappingMongoConverter | Boot auto-config (DataMongoConfiguration) | mongoMappingContext, mongoCustomConversions |
| mongoHealthContributor | MongoHealthIndicator | Boot auto-config (MongoHealthContributorAutoConfiguration) | the bean factory (looks up the MongoClient) |

## URL -> handler (from /actuator/mappings)

- POST /orders -> OrderController#create(CreateOrderRequest)
- GET /orders/{id} -> OrderController#get(String)
- POST /orders/{id}/actions/{action} -> OrderController#act(String, OrderAction)

## Facts the evidence shows

- I wrote 7 beans (controller, service, clockConfig, clock, OrdersProperties, lifecycleProbe, the repository interface). Spring Data generated the repository class; Boot auto-configuration made every Mongo bean.
- No bean of type TransactionManager exists (only TransactionProperties, TransactionManagerCustomizers, transactionExecutionListeners). Relevant for P10.
- clockConfig is a CGLIB subclass: Spring proxies @Configuration classes so that calling a @Bean method returns the one singleton instead of a new object.
- orderService is the plain class (no proxy) - nothing asked for one.

## Summary in my own words (OWED - fill in)

1.
2.
3.
