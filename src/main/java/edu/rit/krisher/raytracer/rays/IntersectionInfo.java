package edu.rit.krisher.raytracer.rays;

import edu.rit.krisher.scene.Material;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Value class representing object parameters at a particular hit location.
 * 
 * @author krisher
 * 
 */
public class IntersectionInfo {

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
   public final Vec3 surfaceNormal = new Vec3();

   /**
    * The tangent vector that defines the azimuth == 0 direction for incident and outgoing rays.
    */
   public final Vec3 tangentVector = new Vec3();



}
