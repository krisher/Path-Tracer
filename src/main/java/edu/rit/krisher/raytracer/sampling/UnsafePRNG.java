package edu.rit.krisher.raytracer.sampling;

import java.util.Random;

/**
 * Thread-unsafe extension of {@link Random}, slight increase in performance by only allowing use of an instance from a
 * single thread.
 * 
 * @author krisher
 * 
 */
public final class UnsafePRNG extends Random {
   private static final long multiplier = 0x5DEECE66DL;
   private static final long addend = 0xBL;
   private static final long mask = (1L << 48) - 1;
   private long seed;

   @Override
   public final void setSeed(final long seed) {
      this.seed = (seed ^ multiplier) & mask;
   }

   @Override
   protected final int next(final int bits) {
      seed = (seed * multiplier + addend) & mask;
      return (int) (seed >>> (48 - bits));
   }
}