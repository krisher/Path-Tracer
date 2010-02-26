/**
 * 
 */
package edu.rit.krisher.scene;

import edu.rit.krisher.collections.CopyOnWriteArrayList;
import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 *
 */
public class BoundedGeometry implements Geometry {

   private final CopyOnWriteArrayList<Geometry> children = new CopyOnWriteArrayList<Geometry>(Geometry.class);
   
   private final AxisAlignedBoundingBox box = new AxisAlignedBoundingBox();
   private final Vec3 boxCenter = new Vec3();
   private final Vec3 boxSpans = new Vec3();
   
   public void add(Geometry geom) {
      children.add(geom);
      if (children.size() == 1) box.set(geom.getBounds());
      else box.union(geom.getBounds());
      
      boxSpans.set(box.maxXYZ).subtract(box.minXYZ);
      boxCenter.set(box.minXYZ).scaleAdd(boxSpans, 0.5);
      boxSpans.multiply(0.5);
   }
   
   public Geometry[] getChildren() {
      return children.array;
   }

   /* 
    * @see edu.rit.krisher.scene.Geometry#getHitData(edu.rit.krisher.raytracer.rays.HitData, edu.rit.krisher.vecmath.Ray, double)
    */
   @Override
   public void getHitData(HitData data, Ray ray, double isectDist) {
      for (Geometry geom : children) {
         if (isectDist == geom.intersects(ray)) {
            geom.getHitData(data, ray, isectDist);
            return;
         }
      }
   }

   /* 
    * @see edu.rit.krisher.scene.Geometry#intersects(edu.rit.krisher.vecmath.Ray)
    */
   @Override
   public double intersects(Ray ray) {
      if (ray.intersectsBox(boxCenter, boxSpans.x, boxSpans.y, boxSpans.z) > 0) {
         double intersectDist = 0;
         for (Geometry geom : children) {
            final double d = geom.intersects(ray);
            if (d > 0 && (intersectDist <= 0 || d < intersectDist)) {
               intersectDist = d;
            }
         }
         if (intersectDist > 0) return intersectDist;
      }
      return 0;
   }

   /* 
    * @see edu.rit.krisher.scene.Geometry#getBounds()
    */
   @Override
   public AxisAlignedBoundingBox getBounds() {
      return box;
   }
   
   
}
