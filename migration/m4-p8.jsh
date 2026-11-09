import java.util.concurrent.*;
String slowCall(String what, long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { throw new IllegalStateException(e); } return what + "-ok"; }
void sequential() { long t0 = System.nanoTime(); String a = slowCall("price", 300); String b = slowCall("stock", 300); System.out.println("1 sequential: " + a + " " + b + " in " + (System.nanoTime() - t0) / 1_000_000 + " ms"); }
void parallel(ExecutorService ex) { long t0 = System.nanoTime(); CompletableFuture<String> price = CompletableFuture.supplyAsync(() -> slowCall("price", 300), ex); CompletableFuture<String> stock = CompletableFuture.supplyAsync(() -> slowCall("stock", 300), ex); String both = price.thenCombine(stock, (p, s) -> p + " " + s).join(); System.out.println("2 parallel  : " + both + " in " + (System.nanoTime() - t0) / 1_000_000 + " ms"); }
ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
sequential();
parallel(vt);
parallel(vt);
CompletableFuture<String> broken = CompletableFuture.supplyAsync(() -> { if (true) throw new IllegalArgumentException("stock service down"); return "x"; }, vt);
try { broken.join(); } catch (CompletionException e) { System.out.println("3 join threw: " + e.getClass().getSimpleName() + " cause=" + e.getCause()); }
System.out.println("4 fallback  : " + broken.exceptionally(e -> "stock-unknown").join());
CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> slowCall("rate", 2000), vt).orTimeout(500, TimeUnit.MILLISECONDS);
try { slow.join(); } catch (CompletionException e) { System.out.println("5 timeout   : cause=" + e.getCause().getClass().getSimpleName()); }
vt.close();
/exit
