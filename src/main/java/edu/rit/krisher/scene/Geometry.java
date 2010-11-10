package edu.rit.krisher.scene;

import edu.rit.krisher.raytracer.rays.GeometryIntersection;
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
    * Constant for primitiveID indicating that all primitives should be tested for intersection (and the
    * intersection with the closest to the ray origin should be computed).
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
   public void getHitData(Ray ray, IntersectionInfo data);

   /**
    * Computes the smallest positive distance along the ray to the intersection, or returns a value <= 0 if there is no
    * intersection (we are not interested in intersections at the ray origin).
    * 
    * @param ray
    *           A non-null ray to test intersection with.
    * @param intersection
    *           On input, the primitiveID field indicates which primitive to test for intersection, and the t field
    *           indicates the maximum distance along the ray to test. On output, the hitGeometry and t fields should be
    *           updated iff an intersection was found.
    * @return true if an intersection was found with the specified primitive and 0 < distance < intersection.t (and intersection was subsequently
    *         updated), false otherwise.
    */
   public boolean intersectsPrimitive(Ray ray, GeometryIntersection intersection);

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
