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
      final Ray ray = this;
      // final Vec3 rayOrigin = new Vec3(ray.origin);
      // rayOrigin.subtract(center);
      final Vec3 rayDirection = ray.direction;
      double nearIsect = Double.NEGATIVE_INFINITY;
      double farIsect = Double.POSITIVE_INFINITY;

      double t1, t2;
      if (rayDirection.x != 0) {
         t1 = (-xSize - origin.x + center.x) / rayDirection.x;
         t2 = (xSize - origin.x + center.x) / rayDirection.x;
         if (t1 > t2) {
            if (t2 > nearIsect) {
               nearIsect = t2;
            }
            if (t1 < farIsect) {
               farIsect = t1;
            }
         } else {
            if (t1 > nearIsect) {
               nearIsect = t1;
            }
            if (t2 < farIsect) {
               farIsect = t2;
            }
         }
         if (nearIsect > farIsect || farIsect < 0) {
            return 0;
         }
      } else {
         /*
          * Ray runs parallel to x, can only intersect if origin x is between
          * +/- xSize
          */
         if (origin.x + center.x > xSize || origin.x + center.x < -xSize) {
            return 0;
         }
      }

      if (rayDirection.y != 0) {
         t1 = (-ySize - origin.y + center.y) / rayDirection.y;
         t2 = (ySize - origin.y + center.y) / rayDirection.y;
         if (t1 > t2) {
            if (t2 > nearIsect) {
               nearIsect = t2;
            }
            if (t1 < farIsect) {
               farIsect = t1;
            }
         } else {
            if (t1 > nearIsect) {
               nearIsect = t1;
            }
            if (t2 < farIsect) {
               farIsect = t2;
            }
         }
         if (nearIsect > farIsect || farIsect < 0) {
            return 0;
         }
      } else {
         /*
          * Ray runs parallel to y, can only intersect if origin y is between
          * +/- ySize
          */
         if (origin.y + center.y > ySize || origin.y + center.y < -ySize) {
            return 0;
         }
      }

      if (rayDirection.z != 0) {
         t1 = (-zSize - origin.z + center.z) / rayDirection.z;
         t2 = (zSize - origin.z + center.z) / rayDirection.z;
         if (t1 > t2) {
            if (t2 > nearIsect) {
               nearIsect = t2;
            }
            if (t1 < farIsect) {
               farIsect = t1;
            }
         } else {
            if (t1 > nearIsect) {
               nearIsect = t1;
            }
            if (t2 < farIsect) {
               farIsect = t2;
            }
         }
      } else {
         /*
          * Ray runs parallel to z, can only intersect if origin z is between
          * +/- zSize
          */
         if (origin.z + center.z > zSize || origin.z + center.z < -zSize) {
            return 0;
         }
      }
      if (nearIsect > farIsect || farIsect < 0) {
         return 0;
      }
      if (nearIsect < 0) {
         return farIsect;
      }
      return nearIsect;
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

   public double intersectsSphere(final Vec3 center, final double radius) {
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
   public double intersectsTriangle(final Vec3 v0, final Vec3 e1, final Vec3 e2) {
      // Vec3 e1 = v1.subtract(v0);
      // Vec3 e2 = v2.subtract(v0);
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

}
