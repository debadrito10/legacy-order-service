# M4 performance hypotheses (sandbox, JDK 25, 16 cores, port 18080)

## H1: tail latency under load depends on heap size
- Observation: under /io load (400 in flight), p99 was 163.7 ms with a 32 MB heap.
- Hypothesis: a small heap forces frequent young GCs and a full GC; those pauses land on some requests and stretch the tail.
- Prediction: with 512 MB, far fewer pauses, no full GC, lower p99; throughput and p50 almost unchanged (the work is waiting, not CPU).
- Experiment: same server and load, only -Xms/-Xmx changed (32m vs 512m). Evidence: m4-p10-heap-32m.txt, m4-p10-heap-512m.txt, gc-m4-32m.log, gc-m4-512m.log.
- Result: 32 MB: 3732 req/s, p50 101.9, p99 163.7 ms, 40 pauses, 1 full GC, 160 ms total. 512 MB: 3839 req/s, p50 101.9, p99 137.0 ms, 2 pauses, 0 full, 20 ms total.
- Conclusion: supported. Pauses 20x fewer, p99 -16%, throughput +2.8%, p50 identical.
- Limits: one run each; client and server on the same machine.

## H2: CPU-bound work on virtual threads starves lock holders and the HTTP dispatcher
- Observation (P6): /cpu with virtual threads had inFlightPeak 50 (not <= 16 carriers) and 11% lower throughput than platform 20 (1625 vs 1821 req/s).
  Little's law: 31 ms in the handler vs ~10 ms CPU per request.
- Hypothesis: when all 16 carriers are busy computing, a virtual thread that pauses (even while holding the HTTP server's internal lock; JDK 25 unmounts)
  must wait for a free carrier. Requests wait inside the handler, and the single HTTP-Dispatcher stalls behind lock holders.
- Evidence (P9): JFR jdk.JavaMonitorEnter = 229 events; waiter always HTTP-Dispatcher, holder always a virtual handler thread,
  at ServerImpl$Dispatcher.run line 500; waits 10-50 ms; all but 3 during the /cpu run. JFR hot methods: countPrimes 90.7%.
- Prediction for a follow-up test: running countPrimes on a bounded platform pool (newFixedThreadPool(16)) from the virtual handler
  would cut JavaMonitorEnter events on the dispatcher sharply and bring inFlightPeak and throughput close to platform mode.
- What would prove it wrong (in my own words): If the dispatcher still shows about the same number of JavaMonitorEnter waits after countPrimes moves to a platform pool, the carriers were not the cause.
- Status: supported by JFR evidence; follow-up test not run.

