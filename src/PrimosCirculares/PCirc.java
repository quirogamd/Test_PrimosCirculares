package PrimosCirculares;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PCirc {

// Limite superior
  private static final int LIMITE_SUPERIOR = 1000000;
// Cantidad de Hilos segun PC
  private static final int CONTADOR_THREAD = Runtime.getRuntime().availableProcessors();;

  private final Object lock = new Object();
  private final Thread[] thread = new Thread[CONTADOR_THREAD];
  private final List<Integer> PrimoCircular = Collections.synchronizedList(new ArrayList());
  private final BlockingQueue<Integer> queue = new ArrayBlockingQueue(CONTADOR_THREAD, true);
  private final CyclicBarrier barrier = new CyclicBarrier(CONTADOR_THREAD);

  private final AtomicInteger totalHit = new AtomicInteger();

  public void execute() throws InterruptedException {

    final boolean[] nums = new boolean[LIMITE_SUPERIOR + 1];

   // Creo Threads
    for (int tid = 0; tid < CONTADOR_THREAD; tid++) {
      final Thread t = new Thread(new Worker(tid, nums));
      t.setName("Worker-Thread-" + tid);
      thread[tid] = t;
      t.start();
    }

    int root = (int) Math.ceil(Math.sqrt((double) LIMITE_SUPERIOR));
    for (int i = 3; i <= root; i += 2) {
      if (!nums[i]) {
        if (i - 3 < CONTADOR_THREAD) {
          synchronized (lock) {
            queue.add(i);
            lock.wait();
          }
        } else {
          queue.put(i);
        }
      }
    }

    /*
      Cota de fin de cálculos para los subprocesos (-1).
    */
    for (int tid = 0; tid < CONTADOR_THREAD; tid++) {
      queue.put(-1);
    }

    /*
      Se agrega el número 2 como parte de los números primos circulares
    */
    if (LIMITE_SUPERIOR > 2) {
      PrimoCircular.add(2);
    }

    /*
      Esperar a que finalicen los hilos.
    */
    for (int tid = 0; tid < CONTADOR_THREAD; tid++) {
      thread[tid].join();
    }

  }

// Devolver Primos Circulares
  public Set<Integer> getResult() {
    Set<Integer> result = new TreeSet(PrimoCircular);
    return result;
  }

// Resumen de Datos
  public void report() {
    System.out.printf("Cantidad calculados: %d\n", totalHit.intValue());
    System.out.printf("\n*** Resultado ***\n");
    System.out.printf("Cantidad de primos circulares: %d\n", PrimoCircular.size());
    System.out.println("Listado: ");
    Iterator<Integer> it = getResult().iterator();
    while (it.hasNext()) {
      System.out.println(it.next());
    }
  }

  public static void main(String[] args) throws InterruptedException {
	  PCirc pu = new PCirc();
    pu.execute();
    pu.report();
  }


// Buscar Primos Circulares
  private class Worker implements Runnable {

    private final int TID;
    private final boolean[] nums;
    private long startTime;
    private long ssTime1;
    private long ssTime2;

    private int numbersTaken = 0;

    public Worker(int TID, boolean[] nums) {
      this.TID = TID;
      this.nums = nums;
    }

    @Override
    public void run() {

      //Marcar múltiplos
      while (true) {
        if (processNextStep()) {
          break;
        }
      }

      //Esperar a que terminen los demás hilos
      waitSStep();

      //Buscar primos circulares
      searchForCircularNumbers();

      //Imprimir reporte del subproceso
      report();
    }

    // Reporte simple
    private void report() {
      System.out.printf("Thread-[%d] Times: %.2f ms. - %.2f ms. - %.2f ms.| Count %d\n",
              TID, ssTime1 / 1000000.0, ssTime2 / 1000000.0, (ssTime1 + ssTime2) / 1000000.0, numbersTaken);

      totalHit.addAndGet(numbersTaken);
    }

    private void waitSStep() {
      try {
        barrier.await();
      } catch (BrokenBarrierException | InterruptedException ex) {
        Logger.getLogger(PCirc.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

//Buscar Circulares en Primos
    private void searchForCircularNumbers() {
      startTime = System.nanoTime();
      for (int i = 3 + (2 * (TID)); i < LIMITE_SUPERIOR; i += (2 * CONTADOR_THREAD)) {
        if (!nums[i] && filter(i)) {
          int orbit = i;
          boolean primo = true;
          do {
            orbit = orbit(orbit);
            if (orbit == i) {
              break;
            }
            primo = !nums[orbit];
          } while (primo);

          if (primo) {
            PrimoCircular.add(i);
          }
        }
      }
      ssTime2 = System.nanoTime() - startTime;
    }

    // Marcar Multiplos
    private boolean processNextStep() {
      try {
        int step = queue.take();
        if (step == -1) {
          return true;
        }
        startTime = System.nanoTime();
        numbersTaken++;
        for (int j = step; j <= LIMITE_SUPERIOR / step; j++) {
          nums[step * j] = true;
          if (step - 3 <= CONTADOR_THREAD && j == step + 1) {
            synchronized (lock) {
              lock.notify();
            }
          }
        }
        ssTime1 += System.nanoTime() - startTime;
      } catch (InterruptedException ex) {
        Logger.getLogger(PCirc.class.getName()).log(Level.SEVERE, null, ex);
      }
      return false;
    }

    //Filtro Numeros
    private boolean filter(int n) {
      if (n < 10) {
        return true;
      }
      int o = n;
      do {
        int d = o % 10;
        if (d == 1 || d == 3 || d == 7 || d == 9) {
        } else {
          return false;
        }
        o /= 10;
      } while (o > 0);
      return true;
    }
    // Rotacion de Numero.
    private int orbit(int n) {
      int n_10 = n / 10;
      int orbit = n % 10;
      n = n_10;
      while (n != 0) {
        orbit *= 10;
        n /= 10;
      }
      orbit += n_10;
      return orbit;
    }

  }

}

