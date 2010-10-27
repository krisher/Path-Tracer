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

   /**
    * Integrates a piecewise linear function.
    * 
    * @param xValues
    *           The x values with corresponding y values.
    * @param yValues
    *           The y values with corresponding x values.
    * @param xStart
    *           The start of the integration range.
    * @param xEnd
    *           The end of the integration range.
    */
   public static float integrateLinearFunction(final float[] xValues, final float[] yValues, final float xStart,
         final float xEnd) {
      if (xEnd < xValues[0])
         return (xEnd - xStart) * yValues[0];
      if (xStart > xValues[xValues.length - 1]) 
         return (xEnd - xStart) * yValues[xValues.length - 1];
      double sum = 0;
      
      /*
       * Only a single point, assume this is a constant function.
       */
      if (xValues.length == 1) return (xEnd - xStart) * yValues[0];
      /*
       * Assume that the first/last sample value extend to infinity...
       */
      if (xStart < xValues[0])
         sum += yValues[0] * (xValues[0] - xStart);
      if (xEnd > xValues[xValues.length -1])
         sum += yValues[xValues.length - 1] * (xEnd - xValues[xValues.length - 1]);
      
      /*
       * Find the first xValue that is >= xStart
       */
      int currentIdx = 0;
      for (;xValues[currentIdx + 1] < xStart;++currentIdx); //We know from above that at least one xValue is >= xStart.
      
      for (; currentIdx + 1 < xValues.length && xEnd >= xValues[currentIdx]; ++currentIdx) {
         final float xBracketLess = xValues[currentIdx];
         final float xBracketGreater = xValues[currentIdx + 1];
         
         final float segmentStartX = Math.max(xStart, xBracketLess);
         final float segmentEndX = Math.min(xEnd, xBracketGreater);
         
         final float segStartY = (float)lerp((segmentStartX - xBracketLess) / (xBracketGreater - xBracketLess), yValues[currentIdx], yValues[currentIdx + 1]);
         final float segEndY = (float)lerp((segmentEndX - xBracketLess) / (xBracketGreater - xBracketLess), yValues[currentIdx], yValues[currentIdx + 1]);
         
         sum += 0.5 * (segEndY + segStartY) * (segmentEndX - segmentStartX);
      }
      return (float)sum;
   }

}
