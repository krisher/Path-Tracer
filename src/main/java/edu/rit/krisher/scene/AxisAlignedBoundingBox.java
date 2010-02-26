/**
 * 
 */
package edu.rit.krisher.scene;

import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class AxisAlignedBoundingBox {

   public Vec3 minXYZ = new Vec3();
   public Vec3 maxXYZ = new Vec3();
   
   public void union(AxisAlignedBoundingBox other) {
      if (other.minXYZ.x < minXYZ.x) minXYZ.x = other.minXYZ.x;
      if (other.maxXYZ.x > maxXYZ.x) maxXYZ.x = other.maxXYZ.x;
      
      if (other.minXYZ.y < minXYZ.y) minXYZ.y = other.minXYZ.y;
      if (other.maxXYZ.y > maxXYZ.y) maxXYZ.y = other.maxXYZ.y;
      
      if (other.minXYZ.z < minXYZ.z) minXYZ.z = other.minXYZ.z;
      if (other.maxXYZ.z > maxXYZ.z) maxXYZ.z = other.maxXYZ.z;
   }
   
   public void set(AxisAlignedBoundingBox other) {
      this.minXYZ.set(other.minXYZ);
      this.maxXYZ.set(other.maxXYZ);
   }
}
