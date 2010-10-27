package edu.rit.krisher.util;

/**
 * Random mathematical utility functions.
 * 
 * @author krisher
 * 
 */
public final class MathUtils {

   private MathUtils() {
      /*
       * Prevent construction.
       */
   }

   /**
    * Linear interpolation between two values.
    * 
    * @param frac
    *           The fraction of the distance between the two values to interpolate to.
    * @param x1
    *           The percent == 0 value.
    * @param x2
    *           The percent == 1 value.
    * @return A value 'frac' percent between x1 and x2.
    */
   public static final double lerp(final double frac, final double x1, final double x2) {
      return x1 + frac * (x2 - x1);
   }

}
