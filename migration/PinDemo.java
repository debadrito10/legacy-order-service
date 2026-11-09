import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/** M4 pinning demo: 50 virtual threads each hold a lock while they "wait" 100 ms. */
public class PinDemo {
    public static void main(String[] args) throws Exception {
        int tasks = 50;
        System.out.println("java=" + System.getProperty("java.version")
                + " cores=" + Runtime.getRuntime().availableProcessors());

        long t0 = System.nanoTime();
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                ex.submit(() -> {
                    Object myLock = new Object();
                    synchronized (myLock) {      // each thread has its OWN lock: no contention
                        Thread.sleep(100);
                    }
                    return null;
                });
            }
        }
        System.out.printf("synchronized + sleep : %d ms%n", (System.nanoTime() - t0) / 1_000_000);

        t0 = System.nanoTime();
        try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                ex.submit(() -> {
                    ReentrantLock myLock = new ReentrantLock();
                    myLock.lock();
                    try {
                        Thread.sleep(100);
                    } finally {
                        myLock.unlock();
                    }
                    return null;
                });
            }
        }
        System.out.printf("ReentrantLock + sleep: %d ms%n", (System.nanoTime() - t0) / 1_000_000);
    }
}
