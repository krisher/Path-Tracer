/**
 * 
 */
package edu.rit.krisher.vecmath;

import java.util.Arrays;

/**
 * Simple Axis-Aligned Bounding Box represented by the min and max coordinates.
 * <p>
 * This implementation exposes internal state, violating good encapsulation to maximize performance and and minimize
 * memory usage.
 */
public class AxisAlignedBoundingBox {

   /**
    * The minimum and maximum coordinates (A non-null 6-element array). The first three values are the minumum X, Y, and
    * Z components, and the second 3 are the maxima.
    */
   public final double[] xyzxyz;

   /**
    * Creates a new AxisAlignedBoundingBox with max coord negative infinity, and min coord positive infinity.
    */
   public AxisAlignedBoundingBox() {
      xyzxyz = new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
   }

   /**
    * Copy constructor, creates a new AxisAlignedBoundingBox identical to the specified bounds.
    * 
    * @param copy
    *           A non-null AxisAlignedBoundingBox.
    */
   public AxisAlignedBoundingBox(final AxisAlignedBoundingBox copy) {
      this.xyzxyz = copy.xyzxyz.clone();
   }

   /**
    * Creates a new axis aligned bounding box with the specified minimum and maximum coordinates. The components of min
    * should be &lt;= the corresponding component of max.
    * 
    * @param min
    *           The minimum (most negative) coordinate of the bounds.
    * @param max
    *           The maximum (most positive) coordinate of the bounds.
    */
   public AxisAlignedBoundingBox(final Vec3 min, final Vec3 max) {
      this();
      set(min, max);
   }

   public AxisAlignedBoundingBox(final double minX, final double minY, final double minZ, final double maxX,
         final double maxY, final double maxZ) {
      this();
      set(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public final void set(final double minX, final double minY, final double minZ, final double maxX, final double maxY,
         final double maxZ) {
      this.xyzxyz[0] = minX;
      this.xyzxyz[1] = minY;
      this.xyzxyz[2] = minZ;
      this.xyzxyz[3] = maxX;
      this.xyzxyz[4] = maxY;
      this.xyzxyz[5] = maxZ;
   }

   public final void set(final Vec3 min, final Vec3 max) {
      this.xyzxyz[0] = min.x;
      this.xyzxyz[1] = min.y;
      this.xyzxyz[2] = min.z;
      this.xyzxyz[3] = max.x;
      this.xyzxyz[4] = max.y;
      this.xyzxyz[5] = max.z;
   }

   public final void set(final AxisAlignedBoundingBox other) {
      for (int i = 0; i < 6; ++i) {
         this.xyzxyz[i] = other.xyzxyz[i];
      }
   }

   public final void union(final AxisAlignedBoundingBox other) {
      for (int i = 0; i < 3; ++i) {
         if (other.xyzxyz[i] < xyzxyz[i])
            xyzxyz[i] = other.xyzxyz[i];
         if (other.xyzxyz[i + 3] > xyzxyz[i + 3])
            xyzxyz[i + 3] = other.xyzxyz[i + 3];
      }

   }

   public final Vec3 centerPt() {
      return new Vec3(xyzxyz[0] + (xyzxyz[3] - xyzxyz[0]) * 0.5, xyzxyz[1] + (xyzxyz[4] - xyzxyz[1]) * 0.5, xyzxyz[2]
            + (xyzxyz[5] - xyzxyz[2]) * 0.5);
   }

   public final double[] centerArray() {
      return new double[] { xyzxyz[0] + (xyzxyz[3] - xyzxyz[0]) * 0.5, xyzxyz[1] + (xyzxyz[4] - xyzxyz[1]) * 0.5,
            xyzxyz[2] + (xyzxyz[5] - xyzxyz[2]) * 0.5 };
   }

   /**
    * Accessor for the size of the bounds in the x dimension. As long as the policy that minXYZ is always less than
    * maxXYZ is respected, this will always return a positive value.
    * 
    * @return The size of the box in the x dimension.
    */
   public final double xSpan() {
      return xyzxyz[3] - xyzxyz[0];
   }

   /**
    * Accessor for the size of the bounds in the y dimension. As long as the policy that minXYZ is always less than
    * maxXYZ is respected, this will always return a positive value.
    * 
    * @return The size of the box in the y dimension.
    */
   public final double ySpan() {
      return xyzxyz[4] - xyzxyz[1];
   }

   /**
    * Accessor for the size of the bounds in the z dimension. As long as the policy that minXYZ is always less than
    * maxXYZ is respected, this will always return a positive value.
    * 
    * @return The size of the box in the z dimension.
    */
   public final double zSpan() {
      return xyzxyz[5] - xyzxyz[2];
   }

   /**
    * The diagonal length of the box (distance between min and max coordinates).
    * 
    * @return The diagonal length;
    */
   public final double diagonalLength() {
      final double xSpan = (xyzxyz[3] - xyzxyz[0]);
      final double ySpan = (xyzxyz[4] - xyzxyz[1]);
      final double zSpan = (xyzxyz[5] - xyzxyz[2]);

      return Math.sqrt(xSpan * xSpan + ySpan * ySpan + zSpan * zSpan);
   }

   /**
    * Accessor for the surface area of the box, which is the sum of the areas of the 6 faces.
    * 
    * @return The surface area of the bounding box.
    */
   public final double surfaceArea() {
      final double xSpan = (xyzxyz[3] - xyzxyz[0]);
      final double ySpan = (xyzxyz[4] - xyzxyz[1]);
      final double zSpan = (xyzxyz[5] - xyzxyz[2]);
      return 2.0 * (xSpan * ySpan + xSpan * zSpan + ySpan * zSpan);
   }

   /**
    * Accessor for the volume of the box, the product of the size in each dimension.
    * 
    * @return The volume of the box.
    */
   public final double volume() {
      return xSpan() * ySpan() * zSpan();
   }

   /**
    * Ray-box intersection that computes the ray parameters (signed distance from the ray origin) at the intersection
    * point(s), if any.
    * 
    * @param ray
    *           The ray to test the intersection with.
    * @param params
    *           The minimum 2-element array to store the intersection parameters in. If this method returns true, the
    *           first 2 elements of the array will contain the ray parameters of the intersection points.
    * @return <code>true</code> if the ray intersects the box, <code>false</code> if it does not.
    */
   public final boolean rayIntersectsParametric(final Ray ray, final double[] params) {
      return ray.intersectsBoxParametric(params, xyzxyz[0], xyzxyz[1], xyzxyz[2], xyzxyz[3], xyzxyz[4], xyzxyz[5]);
   }

   /*
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "AxisAlignedBoundingBox [min/max XYZ=" + Arrays.toString(xyzxyz) + "]";
   }

   /**
    * Creates an array containing each of the corner vertices of the bounding box, in the order:
    * <ul>
    *  <li>maxX, minY, minZ</li>
    *  <li>minX, minY, minZ</li>
    *  <li>minX, minY, maxZ</li>
    *  <li>maxX, minY, maxZ</li>
    *  
    *  <li>maxX, maxY, minZ</li>
    *  <li>minX, maxY, minZ</li>
    *  <li>minX, maxY, maxZ</li>
    *  <li>maxX, maxY, maxZ</li>
    * </ul>
    * @return A non-null array with 8 vertices == 24 floats.
    */
   public float[] toVertexArrayF() {
      return new float[] {
            (float) xyzxyz[3], (float) xyzxyz[1], (float) xyzxyz[2], 
            (float) xyzxyz[0], (float) xyzxyz[1], (float) xyzxyz[2], 
            (float) xyzxyz[0], (float) xyzxyz[1], (float) xyzxyz[5],
            (float) xyzxyz[3], (float) xyzxyz[1], (float) xyzxyz[5],

            (float) xyzxyz[3], (float) xyzxyz[4], (float) xyzxyz[2], 
            (float) xyzxyz[0], (float) xyzxyz[4],(float) xyzxyz[2], 
            (float) xyzxyz[0], (float) xyzxyz[4], (float) xyzxyz[5], 
            (float) xyzxyz[3], (float) xyzxyz[4], (float) xyzxyz[5] };
   }

}
