package edu.rit.krisher.scene;

import edu.rit.krisher.vecmath.Vec3;

/**
 * Value class representing object parameters at a particular hit location.
 * 
 * @author krisher
 * 
 */
public class MaterialInfo extends GeometryIntersection {
   /**
    * The material at the hit location.
    */
   public Material material;
   /**
    * Material coordinates at the hit location. The meaning of these values is specific to the Material that they
    * parameterize.
    */
   public double[] materialCoords;
   /**
    * The surface normal at the hit location.
    */
   public final Vec3 surfaceNormal;

   /**
    * The tangent vector that defines the azimuth == 0 direction for incident and outgoing rays.
    */
   public final Vec3 tangentVector = new Vec3();

   /**
    * The hit location where the intersection occurred.
    */
   public final Vec3 hitLocation = new Vec3();

   public MaterialInfo() {
      surfaceNormal = new Vec3(0, 1, 0);
   }

   public MaterialInfo(final Vec3 surfaceNormal) {
      this.surfaceNormal = surfaceNormal;
   }

   /**
    * Computes a unit-length vector that is perpendicular to the specified normal vector. The direction in the
    * perpendicular plane is arbitrarily chosen.
    * 
    * @param tangentResult
    *           A non-null vector in which to store the result.
    * @param surfaceNormal
    *           A non-null unit length vector that the computed result will be perpendicular to.
    */
   public static void computeTangentVector(final Vec3 tangentResult, final Vec3 surfaceNormal) {
      /*
       * Construct orthonormal basis with vectors:
       * 
       * surfaceNormal, directionOut, nv
       */
      tangentResult.set(0, 1, 0);
      if (Math.abs(tangentResult.dot(surfaceNormal)) > 0.9) {
         // Small angle, pick a better vector...
         tangentResult.x = -1.0;
         tangentResult.y = 0;
      }
      tangentResult.cross(surfaceNormal).normalize();
   }
}
