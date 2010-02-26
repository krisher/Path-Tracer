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

   public Box(double xSize, double ySize, double zSize, boolean invertNormals) {
      this(xSize, ySize, zSize, new LambertBRDF(Color.white), null, invertNormals);
   }

   public Box(double xSize, double ySize, double zSize, Material mat, boolean invertNormals) {
      this(xSize, ySize, zSize, mat, null, invertNormals);
   }

   public Box(double xSize, double ySize, double zSize, Material mat, Transform transform, boolean invertNormals) {
      this.xSize = xSize / 2.0;
      this.ySize = ySize / 2.0;
      this.zSize = zSize / 2.0;
      setMaterial(mat);
      setTransform(transform);
      this.invertNormals = invertNormals;
   }

   @Override
   public void getHitData(HitData data, Ray ray, double isectDist) {
      Vec3 hitPt = invTransform.transformPoint(ray.getPointOnRay(isectDist));
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
      data.surfaceNormal = isectNormal;
   }

   @Override
   public double intersects(Ray ray) {
      return ray.getTransformedInstance(invTransform).intersectsBox(Vec3.zero, xSize, ySize, zSize);
   }

   public Material getMaterial() {
      return material;
   }

   public void setMaterial(Material material) {
      this.material = material;
   }

   public Transform getTransform() {
      return transform;
   }

   public void setTransform(Transform transform) {
      this.transform = transform == null ? Vec3.zero : transform;
      this.invTransform = transform.inverted();
   }

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
      for (Vec3 corner : corners) {
         transform.transformPoint(corner);
      }
      final AxisAlignedBoundingBox bounds =new AxisAlignedBoundingBox();
      for (int i=1; i < 8; i++) {
         final Vec3 corner = corners[i];
         if (corner.x < bounds.minXYZ.x) bounds.minXYZ.x = corner.x;
         else if (corner.x > bounds.maxXYZ.x) bounds.maxXYZ.x = corner.x;
         
         if (corner.y < bounds.minXYZ.y) bounds.minXYZ.y = corner.y;
         else if (corner.y > bounds.maxXYZ.y) bounds.maxXYZ.y = corner.y;
         
         if (corner.z < bounds.minXYZ.z) bounds.minXYZ.z = corner.z;
         else if (corner.z > bounds.maxXYZ.z) bounds.maxXYZ.z = corner.z;
      }
      return bounds;
      
   }
}
