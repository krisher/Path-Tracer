/**
 * 
 */
package edu.rit.krisher.util;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class MathUtilsTest {

   
   @Test
   public void integrateConstantShouldMatchRectangularArea() {
      final float[] xValues = {-1, 0, 1};
      final float[] yValues = {1,1,1};
      
      /*
       * Full area.
       */
      double result = MathUtils.integrateLinearFunction(xValues, yValues, -1, 1);
      Assert.assertEquals("Integration of constant function failed.", 2, result, 0);
      
      /*
       * Partial area.
       */
      result = MathUtils.integrateLinearFunction(xValues, yValues, -1, 0);
      Assert.assertEquals("Integration of segment of constant function failed.", 1, result, 0);
      
      /*
       * Try an integral range that does not line up with sample locations. 
       */
      result = MathUtils.integrateLinearFunction(xValues, yValues, 0.25f, 0.75f);
      Assert.assertEquals("Integration of segment of constant function failed.", 0.5, result, 0);
   }
   
   @Test
   public void integrateSingleValueShouldMatchRectangularArea() {
      final float[] xValues = {0};
      final float[] yValues = {1};
      
      /*
       * Full area.
       */
      double result = MathUtils.integrateLinearFunction(xValues, yValues, -1, 1);
      Assert.assertEquals("Integration of constant function failed.", 2, result, 0);
      
      /*
       * Partial area.
       */
      result = MathUtils.integrateLinearFunction(xValues, yValues, -1, 0);
      Assert.assertEquals("Integration of segment of constant function failed.", 1, result, 0);
      
      /*
       * Try an integral range that does not line up with sample locations. 
       */
      result = MathUtils.integrateLinearFunction(xValues, yValues, 0.25f, 0.75f);
      Assert.assertEquals("Integration of segment of constant function failed.", 0.5, result, 0);
   }
   
   @Test
   public void integrateSampleRangeShouldExtendToInfinity() {
      final float[] xValues = {-1, 0, 1};
      final float[] yValues = {1,1,1};
      
      final double result = MathUtils.integrateLinearFunction(xValues, yValues, -10, 0);
      Assert.assertEquals("Integration of constant function failed.", 10, result, 0);
   }
   
   @Test
   public void integrateShouldMatchTrapezoidArea() {
      final float[] xValues = {-1, 0, 1};
      final float[] yValues = {0,1,0};
      
      /*
       * Full area.
       */
      double result = MathUtils.integrateLinearFunction(xValues, yValues, -1, 1);
      Assert.assertEquals("Integration of linear function failed.", 1, result, 0);
      
      /*
       * Partial area.
       */
      result = MathUtils.integrateLinearFunction(xValues, yValues, -10, 0);
      Assert.assertEquals("Integration of segment of linear function failed.", 0.5, result, 0);
      
      /*
       * Try an integral range that does not line up with sample locations. 
       */
      result = MathUtils.integrateLinearFunction(xValues, yValues, 0.25f, 0.75f);
      Assert.assertEquals("Integration of segment of constant function failed.", 0.25, result, 0);
   }
}
