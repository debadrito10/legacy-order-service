Verdict: request changes

Model review of PR #4 "Order notes" (written by Claude as a study example for M8 P8; not Sana's own review).

The note feature itself is a reasonable idea and the new field is additive, but this PR contains one breaking change hidden as a "small cleanup", disables the test that guards against it, and adds an endpoint that turns a 404 into a 500 and logs personal data. It cannot merge as is.

1. BLOCKER - OrderResponse.java: breaking change hidden as "cleanup".
   `amountPaise` is renamed to `amount`. Every existing client reads `amountPaise`; after this merge they get no price at all (missing field), with no error. Renaming or removing a response field is a breaking change. Adding `note` is fine.
   Fix: keep `amountPaise`. If a new name is really wanted, add `amount` alongside it, mark `amountPaise` deprecated, and remove it only in a new API version.

2. BLOCKER - OrderResponseContractTests.java: the guard test was edited to pass.
   `orderResponseKeepsItsV1Fields` exists precisely to stop this rename (M8 P5). Changing `$.amountPaise` to `$.amount` makes the build green while the contract breaks. A contract test must never be updated in the same PR that breaks the contract; a failing contract test means "talk to the API owner first".
   Fix: revert this test change.

3. BLOCKER - OrderService.addNote: errors are swallowed.
   `catch (Exception e) { return null; }` hides every failure: unknown order id, database down, version conflict. The controller then calls `OrderResponse.from(null)`, which throws NullPointerException, so a client asking for an unknown order gets 500 instead of the documented 404 Problem Details. It also hides real outages from logs and alerts.
   Fix: remove the try/catch. Let OrderNotFoundException (404) and OptimisticLockingFailureException (409) reach ApiExceptionHandler like every other endpoint.

4. MAJOR - OrderService.addNote: personal data in INFO logs.
   The log line writes the customer name and the full free-text note. Notes are user input and will contain phone numbers, addresses, delivery instructions. INFO logs are kept for weeks and read by many people and tools; this is a privacy/compliance problem (and log injection if the note contains newlines).
   Fix: log only the order id and the note length, e.g. "Note updated on order {} ({} chars)", at DEBUG.

5. MAJOR - OrderController.note: input not validated.
   `@RequestBody String note` accepts any size and any content type: an empty body, or 10 MB of text stored in MongoDB and returned in every order response. Every other input in this API is validated and returns Problem Details.
   Fix: use a request record, e.g. `record NoteRequest(@NotBlank @Size(max = 500) String note)` with `@Valid`, so bad input gets the standard 400 with `errors`.

6. MAJOR - no tests for the new endpoint.
   No test covers PUT /orders/{id}/note: not the happy path, not an unknown id, not an oversized note. "All tests pass" is true only because nothing tests the new code (and because of point 2).
   Fix: add OrderControllerWebTests cases (200 with note, 404 Problem Details for unknown id, 400 for blank/oversized note) and an OrderServiceTests case for addNote.

7. MINOR - inconsistent with the other mutating endpoints.
   Actions return an ETag and accept If-Match (M7 P3); this endpoint returns no ETag and ignores If-Match, so a client cannot do a safe read-modify-write of the note.
   Fix: return through the same `withVersion(...)` helper and accept an optional If-Match like `act`.

8. MINOR - PR description.
   "Small cleanup in the response" hides a breaking change, and "All tests pass" is misleading after a contract test was edited. A description should list every behaviour change, especially anything a client can see.
   Fix: describe the new endpoint, its validation and errors, and state "no breaking changes" only when it is true.

What is good: adding `note` to OrderDocument and OrderResponse is additive and safe; PUT is the right method for "replace the note" (idempotent); the endpoint path is clear.

Checklist used: description, contract changes, validation, error handling, sensitive logging, tests (and edited tests), concurrency, readability.
