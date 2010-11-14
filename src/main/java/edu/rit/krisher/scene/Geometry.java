package edu.rit.krisher.scene;

import edu.rit.krisher.raytracer.rays.GeometryRay;
import edu.rit.krisher.raytracer.rays.IntersectionInfo;
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
    * Constant for primitiveID indicating that all primitives should be tested for intersection (and the intersection
    * with the closest to the ray origin should be computed).
    */
   public static final int ALL_PRIMITIVES = -1;

   /**
    * Given a previously computed intersection distance along the specified ray, compute the hit parameters of the
    * geometry at the intersection point.
    * 
    * @param ray
    *           A ray that has been previously determined to intersect this geometry.
    * @param data
    *           An IntersectionInfo whose geometry intersection information was previously populated by a call to
    *           {@link #intersects(Ray, GeometryIntersection)}. The material, surface normal, and optional material
    *           parameters are populated by this method.
    */
   public void getHitData(GeometryRay ray, IntersectionInfo data);

   /**
    * Computes the smallest positive distance along the ray to the intersection with the specified primitive, or returns
    * a value <= 0 if there is no intersection.
    * 
    * @param ray
    *           A non-null ray to test intersection with. If an intersection is found, t is updated with the new
    *           intersection distance. ray.primitiveID specifies the primitive to check the intersection with.
    */
   public boolean intersectsPrimitive(Ray ray, int primitiveID);

   /**
    * Computes the smallest positive distance along the ray to the intersection and stores the result (intersection
    * distance, geometry and primitiveID) in the {@link GeometryIntersection} parameter. If there is no intersection
    * with <code>0 < distance < intersection.t</code>, nothing is updated and this method returns false.
    * 
    * @param ray
    *           A non-null ray to test intersection with.
    * @param intersection
    *           The intersection info to populate upon successful intersection.
    * 
    * @return true if an intersection was found with 0 < distance < intersection.t (and intersection was subsequently
    *         updated), false otherwise.
    */
   public boolean intersects(GeometryRay ray);

   /**
    * Determines whether the specified ray intersects with this geometry at a distance > 0 and < ray.t.
    * 
    * @param ray
    *           The ray to check for intersection.
    * @return true if the ray intersects with distance > 0 and < ray.t, false otherwise.
    */
   public boolean intersectsP(Ray ray);

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
