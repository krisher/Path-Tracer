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
   public final Vec3 surfaceNormal = new Vec3(0,1,0);

}