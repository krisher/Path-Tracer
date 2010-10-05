package edu.rit.krisher.scene.geometry;

import edu.rit.krisher.raytracer.rays.HitData;
import edu.rit.krisher.scene.AxisAlignedBoundingBox;
import edu.rit.krisher.scene.Geometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.scene.material.LambertBRDF;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Transform;
import edu.rit.krisher.vecmath.Vec3;

public class Box implements Geometry {

   private final double xSize;
   private final double ySize;
   private final double zSize;

   private Material material;

   private Transform transform;
   private Transform invTransform;
   private boolean invertNormals = false;

   public Box(final double xSize, final double ySize, final double zSize, final boolean invertNormals) {
      this(xSize, ySize, zSize, new LambertBRDF(Color.white), null, invertNormals);
   }

   public Box(final double xSize, final double ySize, final double zSize, final Material mat, final boolean invertNormals) {
      this(xSize, ySize, zSize, mat, null, invertNormals);
   }

   public Box(final double xSize, final double ySize, final double zSize, final Material mat, final Transform transform, final boolean invertNormals) {
      this.xSize = xSize / 2.0;
      this.ySize = ySize / 2.0;
      this.zSize = zSize / 2.0;
      setMaterial(mat);
      setTransform(transform);
      this.invertNormals = invertNormals;
   }

   @Override
   public void getHitData(final HitData data, final Ray ray, final double isectDist) {
      final Vec3 hitPt = invTransform.transformPoint(ray.getPointOnRay(isectDist));
      // Figure out which face the intersection occurred on
      Vec3 isectNormal;
      final double xDist = Math.abs(Math.abs(hitPt.x) - xSize);
      final double yDist = Math.abs(Math.abs(hitPt.y) - ySize);
      final double zDist = Math.abs(Math.abs(hitPt.z) - zSize);
      if (xDist < yDist) {
         if (xDist < zDist) {
            // face perpendicular to x
            if (hitPt.x < 0) isectNormal = Vec3.xAxis.inverted();
            else 
               isectNormal = Vec3.xAxis;
         } else {
            // face perpendicular to z
            if (hitPt.z < 0) isectNormal = Vec3.zAxis.inverted();
            else 
               isectNormal = Vec3.zAxis;
         }
      } else if (yDist < zDist) {
         // face perpendicular to y
         if (hitPt.y < 0) isectNormal = Vec3.yAxis.inverted();
         else 
            isectNormal = Vec3.yAxis;
      } else {
         // face perpendicular to z
         if (hitPt.z < 0) isectNormal = Vec3.zAxis.inverted();
         else 
            isectNormal = Vec3.zAxis;
      }
      isectNormal = transform.transformVec(new Vec3(isectNormal));
      if (invertNormals) //isectNormal.dot(ray.direction) > 0
         isectNormal.multiply(-1);

      data.material = material;
      data.materialCoords = new double[] { hitPt.x, hitPt.y, hitPt.z };
      data.surfaceNormal.set(isectNormal);
   }

   @Override
   public double intersects(final Ray ray) {
      return ray.getTransformedInstance(invTransform).intersectsBox(Vec3.zero, xSize, ySize, zSize);
   }

   public Material getMaterial() {
      return material;
   }

   public void setMaterial(final Material material) {
      this.material = material;
   }

   public Transform getTransform() {
      return transform;
   }

   public void setTransform(final Transform transform) {
      this.transform = transform == null ? Vec3.zero : transform;
      this.invTransform = this.transform.inverted();
   }

   @Override
   public double getSurfaceArea() {
      /*
       * The 4.0 is because the size value represent distance from the center; half the size.
       */
      return 2.0 * 4.0 * (xSize * ySize + xSize * zSize + ySize * zSize);
   }

   @Override
   public AxisAlignedBoundingBox getBounds() {
      final Vec3[] corners = new Vec3[8];
      corners[0] = new Vec3(-xSize, -ySize, -zSize);
      corners[1] = new Vec3(-xSize, -ySize, zSize);
      corners[2] = new Vec3(-xSize, ySize, -zSize);
      corners[3] = new Vec3(xSize, -ySize, -zSize);
      corners[4] = new Vec3(xSize, ySize, -zSize);
      corners[5] = new Vec3(-xSize, ySize, zSize);
      corners[6] = new Vec3(xSize, -ySize, zSize);
      corners[7] = new Vec3(xSize, ySize, zSize);
      for (final Vec3 corner : corners) {
         transform.transformPoint(corner);
      }
      final AxisAlignedBoundingBox bounds =new AxisAlignedBoundingBox();
      for (int i=1; i < 8; i++) {
         final Vec3 corner = corners[i];
         if (corner.x < bounds.minXYZ[0]) bounds.minXYZ[0] = corner.x;
         else if (corner.x > bounds.maxXYZ[0]) bounds.maxXYZ[0] = corner.x;

         if (corner.y < bounds.minXYZ[1]) bounds.minXYZ[1] = corner.y;
         else if (corner.y > bounds.maxXYZ[1]) bounds.maxXYZ[1] = corner.y;

         if (corner.z < bounds.minXYZ[2]) bounds.minXYZ[2] = corner.z;
         else if (corner.z > bounds.maxXYZ[2]) bounds.maxXYZ[2] = corner.z;
      }
      return bounds;

   }

   @Override
   public Geometry[] getPrimitives() {
      return new Geometry[] { this };
   }
}
