Thread v = Thread.ofVirtual().unstarted(() -> {});
System.out.println("isVirtual=" + v.isVirtual() + "  " + v);
Thread p = Thread.ofPlatform().unstarted(() -> {});
System.out.println("isVirtual=" + p.isVirtual() + "  " + p);
long run(int n, boolean virtual) throws Exception {
    long t0 = System.nanoTime();
    java.util.List<Thread> all = new java.util.ArrayList<>();
    for (int i = 0; i < n; i++) {
        Runnable sleeper = () -> { try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } };
        all.add(virtual ? Thread.ofVirtual().start(sleeper) : Thread.ofPlatform().start(sleeper));
    }
    for (Thread t : all) t.join();
    return (System.nanoTime() - t0) / 1_000_000;
}
System.out.println("10000 virtual  x 1 s sleep: " + run(10_000, true) + " ms");
System.out.println("10000 platform x 1 s sleep: " + run(10_000, false) + " ms");
System.out.println("100000 virtual x 1 s sleep: " + run(100_000, true) + " ms");
/exit
