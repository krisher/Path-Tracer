/**
 * 
 */
package edu.rit.krisher.scene;

import edu.rit.krisher.raytracer.rays.GeometryRay;
import edu.rit.krisher.vecmath.AxisAlignedBoundingBox;

/**
 *
 */
public interface GeometryContainer {

   /**
    * Computes the smallest positive distance along the ray to the intersection and stores the result (intersection
    * distance, geometry and primitiveID). If there is no intersection with <code>0 < distance < intersection.t</code>,
    * nothing is updated and this method returns false.
    * 
    * @param ray
    *           A non-null ray to test intersection with, on output the hitGeometry, primitiveID, and t fields should be
    *           updated to reflect a successful intersection.
    * 
    * @return true if an intersection was found with 0 < distance < intersection.t (and ray fields were subsequently
    *         updated), false otherwise.
    */
   public boolean intersects(GeometryRay ray);

   /**
    * Accessor for a tight fitting axis-aligned bounding box around the geometry.
    * 
    * @return A non-null bounding box.
    */
   public AxisAlignedBoundingBox getBounds();

   /**
    * Accessor for all of the geometry that makes up the scene.
    * 
    * @return A non-null array of geometry.
    */
   public Geometry[] getGeometry();
}
