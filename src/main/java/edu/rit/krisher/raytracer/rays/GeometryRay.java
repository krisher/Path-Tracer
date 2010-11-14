package edu.rit.krisher.raytracer.rays;

import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Ray with additional information regarding which geometric primitive was actually intersected.
 * 
 * @author krisher
 * 
 */
public class GeometryRay extends Ray {

   /**
    * Integer primitive ID within the {@link Geometry} instance indicating which primitive the intersection occurred in.
    */
   public int primitiveID = Geometry.ALL_PRIMITIVES;

   /**
    * The geometry instance that was hit.
    */
   public Geometry hitGeometry;

   /**
    * Creates a new Geometry ray with the specified origin and direction.
    * 
    * @param origin
    *           A non-null origin for the ray.
    * @param direction
    *           A non-null, unit length vector.
    */
   public GeometryRay(final Vec3 origin, final Vec3 direction) {
      super(origin, direction);
   }

}
