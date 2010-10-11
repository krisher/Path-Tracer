package edu.rit.krisher.scene.camera;

import java.util.Random;

import edu.rit.krisher.scene.Camera;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

public class DoFCamera extends PinholeCamera implements Camera {
   private double aperture = 1 / 8.0;
   private double focalDist = 5;

   public double getAperture() {
      return aperture;
   }

   public void setAperture(final double aperture) {
      this.aperture = aperture;
   }

   public double getFocalDist() {
      return focalDist;
   }

   public void setFocalDist(final double focalDist) {
      this.focalDist = focalDist;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.rit.krisher.scene.Camera#generateRays(double, double, double)
    */
   @Override
   public void initSampleRay(final Ray rayOut, final double x, final double y, final Random rng) {
      /*
       * X and Y sample locations will be scaled so they correspond to locations
       * on the focal plane.
       */
      final double focalX = x * focalDist / rayZDist;
      final double focalY = y * focalDist / rayZDist;
      /*
       * Assuming the center of the lense is at 0,0,0, find the intersection of
       * a ray from the lense center with the focal plane, and transform to
       * world coordinates.
       */
      orientation.transformVec(rayOut.direction.set(focalX, focalY, -focalDist)).add(position);

      /*
       * Jittered sample distance from the center of the lense with diameter
       * 'aperture' (focalLength / aperture #)
       */
      final double r = 0.5 * aperture * rng.nextDouble();
      /*
       * Jittered sample angle around the lense
       */
      final double sampleJittered = 2 * Math.PI * (rng.nextDouble());
      final double sampleX = r * Math.cos(sampleJittered);
      final double sampleY = r * Math.sin(sampleJittered);

      /*
       * Ray origin is computed by transforming the x/y sample (assuming a 0,0,0
       * lense center and view looking down the -z axis) to world coordinates.
       */
      orientation.transformVec(rayOut.origin.set(sampleX, sampleY, 0)).add(this.position);

      /*
       * The ray direction is a vector from the jittered origin to the
       * convergence point
       */
      rayOut.direction.subtract(rayOut.origin).normalize();
   }

   /*
    * @see
    * edu.rit.krisher.scene.camera.PinholeCamera#lookAt(edu.rit.krisher.vecmath
    * .Vec3, double, double, double)
    */
   @Override
   public void lookAt(final Vec3 target, final double elevationDeg, final double azimuthDeg, final double distance) {
      super.lookAt(target, elevationDeg, azimuthDeg, distance);
      setFocalDist(distance);
   }

}
