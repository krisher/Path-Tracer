package edu.rit.krisher.scene.geometry.buffer;

import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Buffer that contains 3D vector data.
 * 
 * @author krisher
 * 
 */
public interface Vec3Buffer extends Buffer {

   public Vec3Buffer get(Vec3 value);

   public Vec3Buffer get(int idx, Vec3 value);

   public Vec3Buffer put(Vec3 value);

   public Vec3Buffer put(double x, double y, double z);

   public Vec3Buffer put(int idx, Vec3 value);

   /**
    * Fetches the vec3 values specified by the indices into the output array.
    * 
    * @param value
    *           A non-null array with length of at least indices.length * 3 to store the results.
    * @param indices
    *           A non-null list of Vec3 indices to fetch.
    * @return this.
    */
   public Vec3Buffer get(double[] value, int... indices);

   public AxisAlignedBoundingBox computeBounds();

   /**
    * Computes the bounding box of the vectors corresponding to the specified indices.
    * 
    * @param indices
    *           non-empty array of indices indicating which Vec3's to compute the bounds from.
    * @return A non-null bounding box tightly enclosing the specified vec3 locations.
    */
   public AxisAlignedBoundingBox computeBounds(int... indices);
}
