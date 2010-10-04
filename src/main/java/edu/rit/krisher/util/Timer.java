package edu.rit.krisher.util;

import java.io.PrintStream;

public class Timer {

   private final String name;

   private long accumTime;
   private final ThreadLocal<Long> startedTime = new ThreadLocal<Long>();

   public Timer(final String name) {
      this.name = name;
   }

   public Timer start() {
      if (isRunning())
         throw new IllegalStateException("Timer " + name + " already running!");
      startedTime.set(System.nanoTime());
      return this;
   }

   public Timer reset() {
      if (isRunning())
         stop();
      this.accumTime = 0;
      return this;
   }

   public Timer stop() {
      final long nanoTime = System.nanoTime();
      if (!isRunning())
         throw new IllegalStateException("Timer " + name + " not running!");
      synchronized (this) {
         accumTime += nanoTime - startedTime.get();
      }
      startedTime.set(null);
      return this;
   }

   public boolean isRunning() {
      return (startedTime.get() != null);
   }

   public Timer print(final PrintStream out) {
      final long nanoTime = System.nanoTime();
      if (isRunning()) {
         synchronized (this) {
            accumTime += nanoTime - startedTime.get();
         }
      }
      System.out.println("Time [" + name + "]: " + accumTime / 1000000.0 + "ms");
      if (isRunning()) {
         startedTime.set(System.nanoTime());
      }
      return this;
   }
}
