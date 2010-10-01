/**
 * 
 */
package edu.rit.krisher.scene;

import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public final class AxisAlignedBoundingBox {

   public final double[] minXYZ;
   public final double[] maxXYZ;

   public AxisAlignedBoundingBox() {
      minXYZ = new double[3];
      maxXYZ = new double[3];
   }

   public AxisAlignedBoundingBox(final AxisAlignedBoundingBox copy) {
      this.minXYZ = copy.minXYZ.clone();
      this.maxXYZ = copy.maxXYZ.clone();
   }

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

   public Vec3 center() {
      return new Vec3(minXYZ[0] + (maxXYZ[0] - minXYZ[0]) * 0.5, minXYZ[1] + (maxXYZ[1] - minXYZ[1]) * 0.5, minXYZ[2]
            + (maxXYZ[2] - minXYZ[2]) * 0.5);
   }

   public double xSpan() {
      return maxXYZ[0] - minXYZ[0];
   }

   public double ySpan() {
      return maxXYZ[1] - minXYZ[1];
   }

   public double zSpan() {
      return maxXYZ[2] - minXYZ[2];
   }

   public double diagonalLength() {
      final double xSpan = (maxXYZ[0] - minXYZ[0]);
      final double ySpan = (maxXYZ[1] - minXYZ[1]);
      final double zSpan = (maxXYZ[2] - minXYZ[2]);

      return Math.sqrt(xSpan * xSpan + ySpan * ySpan + zSpan * zSpan);
   }

   public boolean rayIntersectsParametric(final Ray ray, final double[] params) {
      return ray.intersectsBoxParametric(params, minXYZ[0], minXYZ[1], minXYZ[2], maxXYZ[0], maxXYZ[1], maxXYZ[2]);
   }

   /*
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "AxisAlignedBoundingBox [minXYZ=" + minXYZ + ", maxXYZ=" + maxXYZ + "]";
   }

}
