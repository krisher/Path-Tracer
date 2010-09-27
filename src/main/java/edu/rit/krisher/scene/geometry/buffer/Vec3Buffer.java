package edu.rit.krisher.scene.geometry.buffer;

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

   public Vec3Buffer put(int idx, Vec3 value);
}
