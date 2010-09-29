package edu.rit.krisher.vecmath;

public class Plane {

   public double a;
   public double b;
   public double c;
   public double d;

   public Plane() {
   }

   public Plane(final double a, final double b, final double c, final double d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
   }

   public void set(final double a, final double b, final double c, final double d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
   }

   /**
    * Signed distance of point from plane.
    * 
    * @param x
    * @param y
    * @param z
    * @return
    */
   public double distance(final double x, final double y, final double z) {
      final double normalLenSq = a * a + b * b + c * c;
      if (Math.abs(normalLenSq - 1.0) < Float.MIN_VALUE) {
         return (x * a + y * b + z * c + d);
      }
      return (x * a + y * b + z * c + d) / Math.sqrt(normalLenSq);
   }
}
