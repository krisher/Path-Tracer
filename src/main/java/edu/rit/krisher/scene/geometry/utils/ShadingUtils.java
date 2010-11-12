/**
 * 
 */
package edu.rit.krisher.scene.geometry.utils;

import edu.rit.krisher.vecmath.Vec3;

/**
 * Utility methods for shading/material implementation.
 */
public class ShadingUtils {

   /**
    * Converts a vector specified in shading coordinates (z == surface normal, x, y are material dependent).
    * 
    * @param vec
    *           Contains the shading coordinates on entry, the world coordinates on output.
    * @param shadingNormal
    *           A vector representing the world-space normal to the shading point.
    * @param shadingX
    *           A vector representing the world-space x-axis in the tangent plane of the shading point.
    */
   public static final void shadingCoordsToWorld(final Vec3 vec, final Vec3 shadingNormal, final Vec3 shadingX) {
      final double yAxisX = shadingNormal.y * shadingX.z - shadingNormal.z * shadingX.y;
      final double yAxisY = shadingNormal.z * shadingX.x - shadingNormal.x * shadingX.z;
      final double yAxisZ = shadingNormal.x * shadingX.y - shadingNormal.y * shadingX.x;

      vec.set(vec.x * shadingX.x + vec.y * yAxisX + vec.z * shadingNormal.x, vec.x * shadingX.y + vec.y * yAxisY
              + vec.z * shadingNormal.y, vec.x * shadingX.z + vec.y * yAxisZ + vec.z * shadingNormal.z);
   }

   /**
    * Converts a vector specified in world coordinates to shading coordinates (z == surface normal, x, y are material
    * dependent).
    * 
    * @param vec
    *           Contains the world coordinates on entry, the shading coordinates on output.
    * @param shadingNormal
    *           A vector representing the world-space normal to the shading point.
    * @param shadingX
    *           A vector representing the world-space x-axis in the tangent plane of the shading point.
    */
   public static final void worldCoordsToShading(final Vec3 vec, final Vec3 shadingNormal, final Vec3 shadingX) {
      /*
       * Dot product of vec with y shading axis (normal x shadingX)
       */
      final double y = vec.x * (shadingNormal.y * shadingX.z - shadingNormal.z * shadingX.y) + vec.y
      * (shadingNormal.z * shadingX.x - shadingNormal.x * shadingX.z) + vec.z
      * (shadingNormal.x * shadingX.y - shadingNormal.y * shadingX.x);

      vec.set(vec.dot(shadingX), y, vec.dot(shadingNormal));
   }
}
