package edu.rit.krisher.scene.light;

import java.util.Random;

import edu.rit.krisher.scene.EmissiveGeometry;
import edu.rit.krisher.scene.Material;
import edu.rit.krisher.scene.geometry.Sphere;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class SphereLight extends Sphere implements EmissiveGeometry {

   public SphereLight(Vec3 center, double radius, Material material) {
      super(center, radius, material);
   }

   public SphereLight(Vec3 center, double radius, Color emission, double power) {
      super(center, radius, new Color(emission).multiply(power));
   }

   @Override
   public double sampleEmissiveRadiance(final Vec3 directionOut, Color radianceOut, Vec3 normalOut, final Vec3 origin,
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

      directionOut.multiply(cosRandomAzimuth).scaleAdd(nu, Math.cos(randomPolar) * sinRandomAzimuth)
                  .scaleAdd(nv, Math.sin(randomPolar) * sinRandomAzimuth);

      material.getEmissionColor(radianceOut, directionOut, directionOut.inverted(), null);

      final Ray emissionSampler = new Ray(origin, directionOut);
      final double isectDist = emissionSampler.intersectsSphere(center, radius);
      normalOut.set(directionOut).multiply(isectDist).add(origin).subtract(center).multiply(1.0 / radius);

      /*
       * Multiply by 1/distribution of light samples
       */
      radianceOut
                 .multiply((isectDist * isectDist * (2.0 * Math.PI * (1.0 - cosMaxAngle)) / -normalOut
                                                                                                      .dot(directionOut)));
      return isectDist;
   }

}
