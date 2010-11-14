package edu.rit.krisher.raytracer.rays;

import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class GeometryRay extends Ray {

   /**
    * Integer primitive ID within the {@link Geometry} instance indicating which primitive the intersection occurred in.
    */
   public int primitiveID = Geometry.ALL_PRIMITIVES;

   /**
    * The geometry instance that was hit.
    */
   public Geometry hitGeometry;

   public GeometryRay(final Vec3 origin, final Vec3 direction) {
      super(origin, direction);
   }

}
