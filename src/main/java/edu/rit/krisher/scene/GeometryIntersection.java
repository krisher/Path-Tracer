/**
 * 
 */
package edu.rit.krisher.scene;

/**
 * Value class to store the results of intersection testing between a ray and a geometric primitive.
 */
public class GeometryIntersection {

   /**
    * Integer primitive ID within the {@link Geometry} instance indicating which primitive the intersection occurred in.
    */
   public int primitiveID = Geometry.ALL_PRIMITIVES;

   /**
    * The geometry instance that was hit.
    */
   public Geometry hitGeometry;

   /**
    * The parametric hit location where the intersection occurred.
    */
   public double t = 0;

}
