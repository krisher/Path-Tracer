/**
 * 
 */
package edu.rit.krisher.scene;

import java.util.Arrays;

import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Simple Axis-Aligned Bounding Box represented by the min and max coordinates.
 * <p>
 * This implementation exposes internal state, violating good encapsulation design, to maximize performance and
 * flexibility.
 */
public final class AxisAlignedBoundingBox {

   /**
    * The minimum coordinate (A non-null 3-element array). The values in this array must be less than or equal to the
    * corresponding values in maxXYZ.
    */
   public final double[] minXYZ;
   /**
    * The maximum coordinate (A non-null 3-element array). The values in this array must be greater than or equal to the
    * corresponding values in minXYZ.
    */
   public final double[] maxXYZ;

   /**
    * Creates a new AxisAlignedBoundingBox with coordinates initialized to all 0's.
    */
   public AxisAlignedBoundingBox() {
      minXYZ = new double[3];
      maxXYZ = new double[3];
   }

   /**
    * Copy constructor, creates a new AxisAlignedBoundingBox identical to the specified bounds.
    * 
    * @param copy
    *           A non-null AxisAlignedBoundingBox.
    */
   public AxisAlignedBoundingBox(final AxisAlignedBoundingBox copy) {
      this.minXYZ = copy.minXYZ.clone();
      this.maxXYZ = copy.maxXYZ.clone();
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
      this.minXYZ[0] = minX;
      this.minXYZ[1] = minY;
      this.minXYZ[2] = minZ;
      this.maxXYZ[0] = maxX;
      this.maxXYZ[1] = maxY;
      this.maxXYZ[2] = maxZ;
   }

   public final void set(final Vec3 min, final Vec3 max) {
      this.minXYZ[0] = min.x;
      this.minXYZ[1] = min.y;
      this.minXYZ[2] = min.z;
      this.maxXYZ[0] = max.x;
      this.maxXYZ[1] = max.y;
      this.maxXYZ[2] = max.z;
   }

   public void set(final AxisAlignedBoundingBox other) {
      for (int i = 0; i < 3; ++i) {
         this.minXYZ[i] = other.minXYZ[i];
         this.maxXYZ[i] = other.maxXYZ[i];
      }
   }

   public void union(final AxisAlignedBoundingBox other) {
      if (other.minXYZ[0] < minXYZ[0])
         minXYZ[0] = other.minXYZ[0];
      if (other.maxXYZ[0] > maxXYZ[0])
         maxXYZ[0] = other.maxXYZ[0];

      if (other.minXYZ[1] < minXYZ[1])
         minXYZ[1] = other.minXYZ[1];
      if (other.maxXYZ[1] > maxXYZ[1])
         maxXYZ[1] = other.maxXYZ[1];

      if (other.minXYZ[2] < minXYZ[2])
         minXYZ[2] = other.minXYZ[2];
      if (other.maxXYZ[2] > maxXYZ[2])
         maxXYZ[2] = other.maxXYZ[2];
   }

   public Vec3 centerPt() {
      return new Vec3(minXYZ[0] + (maxXYZ[0] - minXYZ[0]) * 0.5, minXYZ[1] + (maxXYZ[1] - minXYZ[1]) * 0.5, minXYZ[2]
                                                                                                                   + (maxXYZ[2] - minXYZ[2]) * 0.5);
   }

   public double[] centerArray() {
      return new double[] { minXYZ[0] + (maxXYZ[0] - minXYZ[0]) * 0.5, minXYZ[1] + (maxXYZ[1] - minXYZ[1]) * 0.5,
            minXYZ[2] + (maxXYZ[2] - minXYZ[2]) * 0.5 };
   }

   /**
    * Accessor for the size of the bounds in the x dimension. As long as the policy that minXYZ is always less than
    * maxXYZ is respected, this will always return a positive value.
    * 
    * @return The size of the box in the x dimension.
    */
   public double xSpan() {
      return maxXYZ[0] - minXYZ[0];
   }

   /**
    * Accessor for the size of the bounds in the y dimension. As long as the policy that minXYZ is always less than
    * maxXYZ is respected, this will always return a positive value.
    * 
    * @return The size of the box in the y dimension.
    */
   public double ySpan() {
      return maxXYZ[1] - minXYZ[1];
   }

   /**
    * Accessor for the size of the bounds in the z dimension. As long as the policy that minXYZ is always less than
    * maxXYZ is respected, this will always return a positive value.
    * 
    * @return The size of the box in the z dimension.
    */
   public double zSpan() {
      return maxXYZ[2] - minXYZ[2];
   }

   /**
    * The diagonal length of the box (distance between min and max coordinates).
    * 
    * @return The diagonal length;
    */
   public double diagonalLength() {
      final double xSpan = (maxXYZ[0] - minXYZ[0]);
      final double ySpan = (maxXYZ[1] - minXYZ[1]);
      final double zSpan = (maxXYZ[2] - minXYZ[2]);

      return Math.sqrt(xSpan * xSpan + ySpan * ySpan + zSpan * zSpan);
   }

   /**
    * Accessor for the surface area of the box, which is the sum of the areas of the 6 faces.
    * 
    * @return The surface area of the bounding box.
    */
   public double surfaceArea() {
      final double xSpan = (maxXYZ[0] - minXYZ[0]);
      final double ySpan = (maxXYZ[1] - minXYZ[1]);
      final double zSpan = (maxXYZ[2] - minXYZ[2]);
      return 2.0 * (xSpan * ySpan + xSpan * zSpan + ySpan * zSpan);
   }

   /**
    * Accessor for the volume of the box, the product of the size in each dimension.
    * 
    * @return The volume of the box.
    */
   public double volume() {
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
   public boolean rayIntersectsParametric(final Ray ray, final double[] params) {
      return ray.intersectsBoxParametric(params, minXYZ[0], minXYZ[1], minXYZ[2], maxXYZ[0], maxXYZ[1], maxXYZ[2]);
   }

   /*
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "AxisAlignedBoundingBox [minXYZ=" + Arrays.toString(minXYZ) + ", maxXYZ=" + Arrays.toString(maxXYZ) + "]";
   }

}
