# M3 comparison - Java 8 style vs modern Java in legacy-order-service

Question this answers: when should a modern feature be used, and when should it NOT be introduced during a migration?

| Refactor | Java 8 way | Modern way | Lowest JDK | Readability | Risk | In a JDK-migration PR? |
|---|---|---|---|---|---|---|
| OrderDto (P3) | class with getters, hand-written equals/hashCode | record + compact constructor | 16 | high | libraries older than ~2020 may not understand records (Jackson < 2.12; JAXB never) | no - separate PR after the JDK switch |
| Order state (P4) | switch with break + default | switch expression, no default | 14 | high; a missing case is a compile error | low | no |
| Export result (P5) | return String, throws JAXBException | sealed interface + record-pattern switch | 21 | medium-high | every caller must change | no |
| Repository (P6) | loops, null for "not found" | streams, Optional, toList() | 16 | medium | toList() is unmodifiable, unlike Collectors.toList() | no |
| Outbound call (P8) | HttpURLConnection | HttpClient + timeouts + sealed result | 11 | high | new threads/pool; timeouts must be chosen | no |
| Jackson 2.9 -> 2.22 (P2) | - | BOM + newest 2.x | 8+ | n/a | JSON contract (guarded by the pinned test) | only as its own commit, behind the pinned test |

## Why none of these belong in the JDK-switch PR
A migration PR should change WHERE the code runs, not HOW it is written. If a test fails, exactly one change must be to blame.
Every feature above also raises the lowest JDK that can run the code (records 16, switch patterns 21), so it would block a rollback to an older JDK.

## Evidence that a modern feature can be the wrong change
- P3: JAXB rejected a record ("does not have a no-arg default constructor"), so Order stays a class and the record is a separate OrderDto.

## Evidence that modern features catch bugs at compile time
- P4: removing CANCELLED -> OrderStateMachine.java:[8,16] "the switch expression does not cover all possible input values".
- P5: adding Skipped -> App.java:[14,32] same message, in the caller.
- Java 8 equivalents (default branch, if/else instanceof) compile silently and fail at run time.

## Java 8 vs modern: state rule (P4)
Java 8:
    switch (from) {
        case NEW:  if (action == PAY) return PAID; if (action == CANCEL) return CANCELLED; break;
        case PAID: if (action == SHIP) return SHIPPED; if (action == CANCEL) return CANCELLED; break;
        default: break;
    }
    throw new IllegalStateException(...);
Modern:
    return switch (from) {
        case NEW -> switch (action) { case PAY -> PAID; case CANCEL -> CANCELLED; case SHIP -> throw notAllowed(from, action); };
        case PAID -> switch (action) { case SHIP -> SHIPPED; case CANCEL -> CANCELLED; case PAY -> throw notAllowed(from, action); };
        case SHIPPED, CANCELLED -> throw notAllowed(from, action);
    };

## Java 8 vs modern: "not found" (P6)
Java 8:  for (Order o : orders) { if (o.getId().equals(id)) return o; } return null;   // caller may forget the null check
Modern:  return orders.stream().filter(o -> o.getId().equals(id)).findFirst();       // Optional forces a decision

## Where the old version was fine
The Java 8 loop in findById is just as fast and easy to read; the gain is only in the return type (Optional instead of null).
