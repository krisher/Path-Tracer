package edu.rit.krisher.vecmath;

public class Ray {
   public static final double SMALL_D = 1e-15;

   public final Vec3 origin;
   public final Vec3 direction;

   /**
    * Creates a new ray with references to the specified vectors for the origin
    * and direction. Note that these vectors should never be modified after
    * creating a Ray with them.
    * 
    * @param origin
    *           An arbitrary vector.
    * @param direction
    *           A normalized direction vector.
    */
   public Ray(final Vec3 origin, final Vec3 direction) {
      this.origin = origin;
      this.direction = direction;
   }

   /**
    * Returns a new Vec3 representing a point on the ray at the specified
    * distance from the ray origin.
    * 
    * @param distance
    * @return
    */
   public final Vec3 getPointOnRay(final double distance) {
      return new Vec3(origin).scaleAdd(direction, distance);
   }

   public Ray getTransformedInstance(final Transform t) {
      return new Ray(t.transformPoint(new Vec3(origin)), t.transformVec(new Vec3(direction)));
   }

   public double intersectsBox(final Vec3 center, final double xSize, final double ySize, final double zSize) {
      final double[] result = new double[2];
      if (intersectsBoxParametric(result, new Vec3(center.x - xSize, center.y - ySize, center.z - zSize), new Vec3(center.x
            + xSize, center.y + ySize, center.z + zSize))) {
         return result[0] > 0 ? result[0] : result[1];
      }
      return 0;
   }

   public boolean intersectsBoxParametric(final double[] paramsOut, final double minX, final double minY,
         final double minZ, final double maxX, final double maxY, final double maxZ) {
      // final Vec3 rayOrigin = new Vec3(ray.origin);
      // rayOrigin.subtract(center);
      paramsOut[0] = Double.NEGATIVE_INFINITY;
      paramsOut[1] = Double.POSITIVE_INFINITY;

      double t1, t2;
      if (direction.x != 0) {
         t1 = (minX - origin.x) / direction.x;
         t2 = (maxX - origin.x) / direction.x;
         if (t1 > t2) {
            paramsOut[0] = t2 > paramsOut[0] ? t2 : paramsOut[0];
            paramsOut[1] = t1 < paramsOut[1] ? t1 : paramsOut[1];
         } else {
            paramsOut[0] = t1 > paramsOut[0] ? t1 : paramsOut[0];
            paramsOut[1] = t2 < paramsOut[1] ? t2 : paramsOut[1];
         }
         if (paramsOut[0] > paramsOut[1] || paramsOut[1] < 0) {
            return false;
         }
      } else {
         /*
          * Ray runs parallel to x, can only intersect if origin x is between
          * +/- xSize
          */
         if (origin.x > maxX || origin.x < minX) {
            return false;
         }
      }

      if (direction.y != 0) {
         t1 = (minY - origin.y) / direction.y;
         t2 = (maxY - origin.y) / direction.y;
         if (t1 > t2) {
            paramsOut[0] = t2 > paramsOut[0] ? t2 : paramsOut[0];
            paramsOut[1] = t1 < paramsOut[1] ? t1 : paramsOut[1];
         } else {
            paramsOut[0] = t1 > paramsOut[0] ? t1 : paramsOut[0];
            paramsOut[1] = t2 < paramsOut[1] ? t2 : paramsOut[1];
         }
         if (paramsOut[0] > paramsOut[1] || paramsOut[1] < 0) {
            return false;
         }
      } else {
         /*
          * Ray runs parallel to x, can only intersect if origin x is between
          * +/- xSize
          */
         if (origin.y > maxY || origin.y < minY) {
            return false;
         }
      }

      if (direction.z != 0) {
         t1 = (minZ - origin.z) / direction.z;
         t2 = (maxZ - origin.z) / direction.z;
         if (t1 > t2) {
            paramsOut[0] = t2 > paramsOut[0] ? t2 : paramsOut[0];
            paramsOut[1] = t1 < paramsOut[1] ? t1 : paramsOut[1];
         } else {
            paramsOut[0] = t1 > paramsOut[0] ? t1 : paramsOut[0];
            paramsOut[1] = t2 < paramsOut[1] ? t2 : paramsOut[1];
         }
         if (paramsOut[0] > paramsOut[1] || paramsOut[1] < 0) {
            return false;
         }
      } else {
         /*
          * Ray runs parallel to x, can only intersect if origin x is between
          * +/- xSize
          */
         if (origin.z > maxZ || origin.z < minZ) {
            return false;
         }
      }
      if (paramsOut[0] > paramsOut[1] || paramsOut[1] < 0) {
         return false;
      }
      return true;
   }

   public boolean intersectsBoxParametric(final double[] paramsOut, final Vec3 minXYZ, final Vec3 maxXYZ) {
      return intersectsBoxParametric(paramsOut, minXYZ.x, minXYZ.y, minXYZ.z, maxXYZ.x, maxXYZ.y, maxXYZ.z);
   }

   public double intersectsPlane(final Vec3 planeNormal, final double planeDist) {
      final double cosAngle = planeNormal.dot(direction);
      /*
       * Check if the ray is (nearly) parallel to the plane
       */
      if (cosAngle < SMALL_D && cosAngle > -SMALL_D) {
         return 0;
      }
      return -(planeNormal.dot(origin) + planeDist) / cosAngle;
   }

   public final double intersectsSphere(final Vec3 center, final double radius) {
      final double tOx = origin.x - center.x;
      final double tOy = origin.y - center.y;
      final double tOz = origin.z - center.z;

      final double originFromCenterDistSq = (tOx * tOx + tOy * tOy + tOz * tOz);
      final double B = tOx * direction.x + tOy * direction.y + tOz * direction.z;
      final double C = originFromCenterDistSq - radius * radius;
      final double D = B * B - C;
      if (D > 0) {
         final double sqrtD = Math.sqrt(D);
         return (sqrtD < -B) ? -B - sqrtD : -B + sqrtD;
      }
      return 0;
   }

   /**
    * Moller-Trumbore intersection test
    * (http://www.graphics.cornell.edu/pubs/1997/MT97.html)
    * 
    * @param v0
    *           A vertex of the triangle.
    * @param e1
    *           A vector from v0 to the second vertex of the triangle.
    * @param e2
    *           A vector from v0 to the third vertex of the triangle.
    * @return The distance from the origin to the intersection point, if <= 0
    *         there was no intersection.
    */
   public final double intersectsTriangle(final Vec3 v0, final Vec3 e1, final Vec3 e2) {
      final Vec3 p = new Vec3(direction).cross(e2);
      final double divisor = p.dot(e1);
      /*
       * Ray nearly parallel to triangle plane...
       */
      if (divisor < SMALL_D && divisor > -SMALL_D) {
         return 0;
      }

      final Vec3 translatedOrigin = new Vec3(origin);
      translatedOrigin.subtract(v0);
      final Vec3 q = new Vec3(translatedOrigin).cross(e1);
      /*
       * Barycentric coords also result from this formulation, which could be useful for interpolating attributes
       * defined at the vertex locations:
       */
      final double e1Factor = p.dot(translatedOrigin) / divisor;
      if (e1Factor < 0 || e1Factor > 1) {
         return 0;
      }

      final double e2Factor = q.dot(direction) / divisor;
      if (e2Factor < 0 || e2Factor + e1Factor > 1) {
         return 0;
      }

      return q.dot(e2) / divisor;
   }

   /**
    * Moller-Trumbore intersection test
    * (http://www.graphics.cornell.edu/pubs/1997/MT97.html)
    * 
    * @param VEE
    *           A (minimum) 9-element double array containing the 3 vector components for each of the triangle base
    *           vertex, and two edges.
    * @return The distance from the origin to the intersection point, if <= 0
    *         there was no intersection.
    */
   public final double intersectsTriangle(final double[] VEE) {
//      final Vec3 p = new Vec3(direction).cross(VEE[6], VEE[7], VEE[8]);
      final double pX = direction.y * VEE[8] - direction.z * VEE[7];
      final double pY = direction.z * VEE[6] - direction.x * VEE[8];
      final double pZ = direction.x * VEE[7] - direction.y * VEE[6];
      final double divisor = Vec3.dot(pX, pY, pZ, VEE[3], VEE[4], VEE[5]);
      /*
       * Ray nearly parallel to triangle plane...
       */
      if (divisor < SMALL_D && divisor > -SMALL_D) {
         return 0;
      }

//      final Vec3 translatedOrigin = new Vec3(origin);
//      translatedOrigin.subtract(VEE[0], VEE[1], VEE[2]);
      final double translatedOriginX = origin.x - VEE[0];
      final double translatedOriginY = origin.y - VEE[1];
      final double translatedOriginZ = origin.z - VEE[2];
      
//      final Vec3 q = new Vec3(translatedOrigin).cross(VEE[3], VEE[4], VEE[5]);
      final double qX = translatedOriginY * VEE[5] - translatedOriginZ * VEE[4];
      final double qY = translatedOriginZ * VEE[3] - translatedOriginX * VEE[5];
      final double qZ = translatedOriginX * VEE[4] - translatedOriginY * VEE[3];
      /*
       * Barycentric coords also result from this formulation, which could be useful for interpolating attributes
       * defined at the vertex locations:
       */
      final double e1Factor = Vec3.dot(pX, pY, pZ, translatedOriginX, translatedOriginY, translatedOriginZ) / divisor;
      if (e1Factor < 0 || e1Factor > 1) {
         return 0;
      }

      final double e2Factor = Vec3.dot(qX, qY, qZ, direction.x, direction.y, direction.z) / divisor;
      if (e2Factor < 0 || e2Factor + e1Factor > 1) {
         return 0;
      }

      return Vec3.dot(qX, qY, qZ, VEE[6], VEE[7], VEE[8]) / divisor;
   }
   
   /**
    * Moller-Trumbore intersection test
    * (http://www.graphics.cornell.edu/pubs/1997/MT97.html)
    * 
    * @param VEE
    *           A (minimum) 9-element double array containing the 3 vector components for each of the triangle base
    *           vertex, and two edges.
    * @return The distance from the origin to the intersection point, if <= 0
    *         there was no intersection.
    */
   public final boolean intersectsTriangleBarycentric(final double[] tuv, final double[] VEE) {
//      final Vec3 p = new Vec3(direction).cross(VEE[6], VEE[7], VEE[8]);
      final double pX = direction.y * VEE[8] - direction.z * VEE[7];
      final double pY = direction.z * VEE[6] - direction.x * VEE[8];
      final double pZ = direction.x * VEE[7] - direction.y * VEE[6];
      final double divisor = Vec3.dot(pX, pY, pZ, VEE[3], VEE[4], VEE[5]);
      /*
       * Ray nearly parallel to triangle plane...
       */
      if (divisor < SMALL_D && divisor > -SMALL_D) {
         return false;
      }

//      final Vec3 translatedOrigin = new Vec3(origin);
//      translatedOrigin.subtract(VEE[0], VEE[1], VEE[2]);
      final double translatedOriginX = origin.x - VEE[0];
      final double translatedOriginY = origin.y - VEE[1];
      final double translatedOriginZ = origin.z - VEE[2];
      
//      final Vec3 q = new Vec3(translatedOrigin).cross(VEE[3], VEE[4], VEE[5]);
      final double qX = translatedOriginY * VEE[5] - translatedOriginZ * VEE[4];
      final double qY = translatedOriginZ * VEE[3] - translatedOriginX * VEE[5];
      final double qZ = translatedOriginX * VEE[4] - translatedOriginY * VEE[3];
      /*
       * Barycentric coords also result from this formulation, which could be useful for interpolating attributes
       * defined at the vertex locations:
       */
      final double e1Factor = Vec3.dot(pX, pY, pZ, translatedOriginX, translatedOriginY, translatedOriginZ) / divisor;
      if (e1Factor < 0 || e1Factor > 1) {
         return false;
      }

      final double e2Factor = Vec3.dot(qX, qY, qZ, direction.x, direction.y, direction.z) / divisor;
      if (e2Factor < 0 || e2Factor + e1Factor > 1) {
         return false;
      }

      tuv[0] = Vec3.dot(qX, qY, qZ, VEE[6], VEE[7], VEE[8]) / divisor;
      tuv[1] = e1Factor;
      tuv[2] = e2Factor;
      return true;
   }

   @Override
   public String toString() {
      return "Ray [origin=" + origin + ", direction=" + direction + "]";
   }

}
