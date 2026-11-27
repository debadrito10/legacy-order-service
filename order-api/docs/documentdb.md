# Amazon DocumentDB vs MongoDB - study notes

Lead (26 Nov): we may move to Amazon DocumentDB (AWS); for now, learn the differences from MongoDB.
Not tested on a real cluster. I checked our code against AWS's documentation.

## What our code uses

Inserts, saves, finds and deletes through Spring Data; the Idempotency-Key as `_id`; `@Version` optimistic locking; one compound index; one sorted, paged query; one conditional delete (M15). No transactions, aggregation pipelines, `$where` or change streams.

## Differences that matter

1. It is not MongoDB underneath. AWS built its own engine that speaks the MongoDB API (4.0, 5.0 and 8.0 compatibility). Same drivers, but behaviour can differ.
2. Engine version decides features. First question for any feature: which engine are we on?
3. Storage is separate from compute: one primary (reads and writes) and up to 15 replicas (reads only), all on one shared storage volume.
4. Replica reads are eventually consistent (AWS: usually under 100 ms behind). Our M15 replay reads the claim right after a duplicate-key error, so it must read from the primary.
5. Retryable writes only from engine 8.0.2. Before that, `retryWrites=false`, so the Idempotency-Key is our only protection against duplicate orders.
6. Write concern is fixed: DocumentDB ignores `w` and `j` and always writes with majority + journal.
7. TLS is on by default. The app needs AWS's `global-bundle.pem` in a Java truststore (links to M18).
8. Not supported or limited: `$where` (no), `cursor.collation()` (8.0 only), `fsyncLock` (not on AWS's supported list, so the M15 slow-database drill can't be repeated there).
9. No local version or Docker image. Our Testcontainers tests prove MongoDB 8.0 behaviour, not DocumentDB.

## Not checked

Index limits, aggregation differences beyond the above, elastic (sharded) clusters.