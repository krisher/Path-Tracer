package edu.rit.krisher.scene.light;

import java.util.Random;

import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.geometry.Sphere;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public final class SphereLight extends Sphere implements EmissiveGeometry {

   public SphereLight(final Vec3 center, final double radius, final Color material) {
      super(center, radius, material);
   }

   public SphereLight(final Vec3 center, final double radius, final Color emission, final double power) {
      super(center, radius, new Color(emission).multiply(power));
   }

   @Override
   public double sampleEmissiveRadiance(final Vec3 directionOut, final Color radianceOut, final Vec3 origin,
         final Random rng) {

      directionOut.set(center).subtract(origin);
      final double lightDist = directionOut.length();
      /*
       * The maximum angle from originToCenter for a ray eminating from origin
       * that will hit the sphere.
       */
      final double sinMaxAngle = radius / lightDist;
      final double cosMaxAngle = Math.sqrt(1 - sinMaxAngle * sinMaxAngle);

      /*
       * Uniform sample density over the solid angle subtended by the sphere
       * wrt. the origin point. Taken from Shirley and Morely book.
       */
      final double cosRandomAzimuth = 1.0 + rng.nextDouble() * (cosMaxAngle - 1.0);
      final double sinRandomAzimuth = Math.sqrt(1.0 - cosRandomAzimuth * cosRandomAzimuth);

      final double randomPolar = 2.0 * Math.PI * rng.nextDouble();

      /*
       * Construct an orthonormal basis around the direction vector
       */
      directionOut.multiply(1.0 / lightDist);
      final Vec3 nu = new Vec3(0, 1, 0);
      final double cosAng = nu.dot(directionOut);
      if (cosAng < -0.9 || cosAng > 0.9) {
         nu.x = 1;
         nu.y = 0;
      }
      nu.cross(directionOut).normalize();
      final Vec3 nv = new Vec3(directionOut).cross(nu);

      directionOut.multiply(cosRandomAzimuth).scaleAdd(nu, Math.cos(randomPolar) * sinRandomAzimuth).scaleAdd(nv, Math.sin(randomPolar)
            * sinRandomAzimuth);

      material.getEmissionColor(radianceOut, directionOut, null);

      final Ray emissionSampler = new Ray(origin, directionOut);
      final double isectDist = emissionSampler.intersectsSphere(center, radius);

      /*
       * Multiply by 1/distribution of light samples.
       * 
       * Note that the cosine theta term (between surface normal and ray direction) is excluded because we would scale by that term anyway. 
       */
      radianceOut.multiply((isectDist * isectDist * (2.0 * Math.PI * (1.0 - cosMaxAngle))));
      return isectDist;
   }

}
