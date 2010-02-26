package edu.rit.krisher.scene;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.vecmath.Ray;

/**
 * Interface for a ray-trace-able geometric object.
 * 
 * @see EmissiveGeometry for geometric objects that emit light.
 * 
 * @author krisher
 * 
 */
public interface Geometry {

   /**
    * Computes the smallest positive distance along a ray to the intersection
    * with this shape.
    */
   public void getHitData(HitData data, Ray ray, double isectDist);

   /**
    * Computes the smallest positive distance along the ray to the intersection,
    * or returns a value <= 0 if there is no intersection.
    */
   public double intersects(Ray ray);

   public AxisAlignedBoundingBox getBounds();
}
