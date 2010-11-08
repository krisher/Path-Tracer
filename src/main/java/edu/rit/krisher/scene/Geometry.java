package edu.rit.krisher.scene;

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
    * Constant for {@link #primitiveID} indicating that all primitives should be tested for intersection (and the
    * intersection with the closest to the ray origin should be computed).
    */
   public static final int ALL_PRIMITIVES = -1;

   /**
    * Given a previously computed intersection distance along the specified ray, compute the hit parameters of the
    * geometry at the intersection point.
    * 
    * @param data
    *           The material, surface normal, and optional material parameters are populated by this method.
    * @param hitPrimitive
    *           The ID of the primitive that was hit (as indicated via the {@link GeometryIntersection} output from
    *           {@link #intersects(Ray, GeometryIntersection, double)}).
    * @param ray
    *           A ray that has been previously determined to intersect this geometry.
    * @param distance
    *           The distance along the ray at which the intersection occured.
    */
   public void getHitData(IntersectionInfo data, int hitPrimitive, Ray ray, double distance);

   /**
    * Computes the smallest positive distance along the ray to the intersection, or returns a value <= 0 if there is no
    * intersection (we are not interested in intersections at the ray origin).
    * 
    * @param ray
    *           A non-null ray to test intersection with.
    * @param primitiveID
    *           The primitive to check for intersection (no others should be tested) (Do not use {@link #ALL_PRIMITIVES}
    *           here).
    * @param maxDistance
    *           the maximum distance along the ray from its origin to check for intersections.
    * @return the distance along the ray (from the origin) at which the intersection occurs.
    */
   public double intersectsPrimitive(Ray ray, double maxDistance, int primitiveID);

   /**
    * Computes the smallest positive distance along the ray to the intersection and stores the result (intersection
    * distance, geometry and primitiveID) in intersection.
    * If there is no intersection with <code>0 < distance < intersection.t</code>, nothing is updated and this method
    * returns false.
    * 
    * @param ray
    *           A non-null ray to test intersection with.
    * @param intersection
    *           The intersection info to populate upon successful intersection.
    * 
    * @return the distance along the ray (from the origin) at which the intersection occurs.
    */
   public boolean intersects(Ray ray, GeometryIntersection intersection);

   /**
    * Accessor for a tight fitting axis-aligned bounding box around the geometry.
    * 
    * @param primitiveID
    *           The ID of a primitive (0 through 'getPrimitiveCount() - 1') to get the bounds for, or
    *           {@link #ALL_PRIMITIVES} for the bounds of the geometry.
    * @return A non-null bounding box.
    */
   public AxisAlignedBoundingBox getBounds(int primitiveID);

   /**
    * Accessor for the surface area of the geometry.
    * 
    * @param primitiveID
    *           The ID of a primitive (0 through 'getPrimitiveCount() - 1') to get the bounds for, or
    *           {@link #ALL_PRIMITIVES} for the bounds of the geometry.
    * @return The surface area. If it is difficult to compute, use the surface area of the bounding box as a rough
    *         approximation.
    */
   public double getSurfaceArea(int primitiveID);

   /**
    * Accessor for the number of primitives that this geometry consists of (values between 0 and this number are used as
    * primitiveIDs in other methods).
    * 
    * @return The number of distinct primitive IDs.
    */
   public int getPrimitiveCount();

}
