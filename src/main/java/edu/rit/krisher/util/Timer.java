package edu.rit.krisher.util;

import java.io.PrintStream;

/**
 * Simple thread-safe timer utility that independently records time spent in one or more threads.
 * 
 * @author krisher
 * 
 */
public final class Timer {

   private final String name;

   private long accumTime;
   private final ThreadLocal<Long> startedTime = new ThreadLocal<Long>();

   /**
    * Creates a new timer with the specified name/description.
    * 
    * @param name
    *           A descriptive name.
    */
   public Timer(final String name) {
      this.name = name;
   }

   /**
    * Starts the timer on the current thread if not already started.
    * 
    * @return this, for command chaining.
    */
   public Timer start() {
      if (isRunning())
         throw new IllegalStateException("Timer " + name + " already running!");
      startedTime.set(System.nanoTime());
      return this;
   }

   /**
    * Resets the accumulated time. Timer should be stopped on all threads before calling this method.
    * 
    * @return this for command chaining
    */
   public Timer reset() {
      synchronized (this) {
         this.accumTime = 0;
      }
      return this;
   }

   /**
    * Stops the timer in the current thread and adds the time elapsed since the previous call to {@link #start()} to the
    * total time.
    * 
    * @return this for command chaining
    */
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

   /**
    * Checks whether the timer is active in the current thread.
    * 
    * @return true if the timer is running in the calling thread, false otherwise.
    */
   public final boolean isRunning() {
      return (startedTime.get() != null);
   }

   public Timer print(final PrintStream out) {
      final long nanoTime;
      synchronized (this) {
         if (isRunning()) {
            nanoTime = System.nanoTime() - startedTime.get() + accumTime;
         } else
            nanoTime = accumTime;
      }
      System.out.println("Time [" + name + "]: " + nanoTime / 1000000.0 + "ms");
      return this;
   }
}
