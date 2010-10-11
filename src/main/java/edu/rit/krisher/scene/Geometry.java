package edu.rit.krisher.scene;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;
import edu.rit.krisher.vecmath.Ray;

/**
 * Interface for a ray-traceable geometric object.
 * 
 * @see EmissiveGeometry for geometric objects that emit light.
 * 
 * @author krisher
 * 
 */
public interface Geometry {

   /**
    * Given a previously computed intersection distance along the specified ray, compute the hit parameters of the
    * geometry at the intersection point.
    * 
    * @param data
    *           Storage for the results of the hit/intersection.
    * @param ray
    *           A ray that has been previously determined to intersect this geometry.
    * @param isectDist
    *           The distance from the origin of the ray at which the intersection occurs.
    */
   public void getHitData(HitData data, Ray ray, double isectDist, int primitiveIndex);

   /**
    * Computes the smallest positive distance along the ray to the intersection,
    * or returns a value <= 0 if there is no intersection (we are not interested in intersections at the ray origin).
    * 
    * @param ray
    *           A non-null ray to test intersection with.
    * @return the distance along the ray (from the origin) at which the intersection occurs.
    */
   public double intersects(GeometryIntersection intersection, Ray ray, int primitiveIndex);

   /**
    * Accessor for a tight fitting axis-aligned bounding box around the geometry.
    * 
    * @return A non-null bounding box.
    */
   public AxisAlignedBoundingBox getBounds(int primitiveIndex);

   /**
    * Accessor for the surface area of the geometry.
    * 
    * @return The surface area. If it is difficult to compute, use the surface area
    *         of the bounding box as a rough approximation.
    */
   public double getSurfaceArea(int primitiveIndex);
   
   public int getPrimitiveCount();

}
