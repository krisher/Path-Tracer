/**
 * 
 */
package edu.rit.krisher.scene;

import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class AxisAlignedBoundingBox implements Cloneable {

   public Vec3 minXYZ = new Vec3();
   public Vec3 maxXYZ = new Vec3();

   public void union(final AxisAlignedBoundingBox other) {
      if (other.minXYZ.x < minXYZ.x)
         minXYZ.x = other.minXYZ.x;
      if (other.maxXYZ.x > maxXYZ.x)
         maxXYZ.x = other.maxXYZ.x;

      if (other.minXYZ.y < minXYZ.y)
         minXYZ.y = other.minXYZ.y;
      if (other.maxXYZ.y > maxXYZ.y)
         maxXYZ.y = other.maxXYZ.y;

      if (other.minXYZ.z < minXYZ.z)
         minXYZ.z = other.minXYZ.z;
      if (other.maxXYZ.z > maxXYZ.z)
         maxXYZ.z = other.maxXYZ.z;
   }

   public void set(final AxisAlignedBoundingBox other) {
      this.minXYZ.set(other.minXYZ);
      this.maxXYZ.set(other.maxXYZ);
   }

   @Override
   public AxisAlignedBoundingBox clone() {
      try {
         final AxisAlignedBoundingBox box = (AxisAlignedBoundingBox) super.clone();
         box.maxXYZ = new Vec3(box.maxXYZ);
         box.minXYZ = new Vec3(box.minXYZ);
         return box;
      } catch (final CloneNotSupportedException cne) {
         assert false : "Clone should be supported!";
      return null;
      }
   }
}
