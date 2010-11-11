package edu.rit.krisher.raytracer.sampling;

import java.awt.Rectangle;
import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.vecmath.Vec3;

public class SamplingUtils {

   /**
    * Initializes the specified vector to a randomly selected direction with equal probability for any direction.
    * 
    * @param vec
    *           A non-null vector to initialize.
    * @param rng
    *           A random number generator.
    */
   public static final void rejectionSphereSample(final Vec3 vec, final Random rng) {
      do {
         vec.x = rng.nextFloat();
         vec.y = rng.nextFloat();
         vec.z = rng.nextFloat();
      } while (vec.lengthSquared() > 1);
      vec.normalize();
   }

   /**
    * Initializes the pixelX and pixelY values of the specified SampleRays based on the specified pixel rectangle,
    * multi-sampling rate, and random number generator for jittering the sample locations.
    * 
    * @param sampleRays
    *           An array of pixelRect.width * pixelRect.height * msGridSize * msGridSize sample rays.
    * @param pixelRect
    *           The pixels for which to initialize rays.
    * @param msGridSize
    *           The multi-sample rate for each pixel. msGridSize * msGridSize rays will be initialized for each pixel
    *           (with different locations within the pixel.
    * @param rng
    *           A random number generator.
    */
   public static final void generatePixelSamples(final SampleRay[] sampleRays, final Rectangle pixelRect,
         final int msGridSize, final Random rng) {
      /*
       * Imaging ray generation.
       */
      int sampleIdx = 0;
      for (int pixelY = 0; pixelY < pixelRect.height; pixelY++) {
         for (int pixelX = 0; pixelX < pixelRect.width; pixelX++) {
            for (int sampleX = 0; sampleX < msGridSize; ++sampleX) {
               for (int sampleY = 0; sampleY < msGridSize; ++sampleY) {
                  /*
                   * Stratified jittered sampling, an eye ray is generated that passes through a random location in a
                   * small square region of the pixel area for each sample.
                   */
                  sampleRays[sampleIdx].pixelX = pixelRect.x + pixelX + (sampleX) / (float)msGridSize + rng.nextFloat()
                        / msGridSize;
                  sampleRays[sampleIdx].pixelY = pixelRect.y + pixelY + (sampleY) / (float)msGridSize + rng.nextFloat()
                        / msGridSize;
                  
                  assert ((int)sampleRays[sampleIdx].pixelX) == pixelX + pixelRect.x;
                  assert ((int)sampleRays[sampleIdx].pixelY) == pixelY + pixelRect.y;
                  ++sampleIdx;
               }
            }
         }
      }
   }

   /**
    * Generates a unit vector in a random direction with uniform probability around the entire sphere of directions.
    * 
    * @param result
    *           A vector to store the result in.
    * @param rng
    *           A random number generator.
    * @return The probability with which the result was generated.
    */
   public static final double uniformSampleSphere(final Vec3 result, final Random rng) {
      result.z = 1.0 - 2.0 * rng.nextDouble();
      final double r = Math.sqrt(Math.max(0., 1. - result.z * result.z));
      final double phi = 2.0 * Math.PI * rng.nextDouble();
      result.x = r * Math.cos(phi);
      result.y = r * Math.sin(phi);
      return 1.0 / (4.0 * Math.PI);
   }
   
   /**
    * Generates a unit vector in a random direction with uniform probability around the hemisphere of directions about the positive z axis.
    * 
    * @param result
    *           A vector to store the result in.
    * @param rng
    *           A random number generator.
    * @return The probability with which the result was generated.
    * @see PBRT implementation.
    */
   public static final double uniformSampleHemisphere(final Vec3 result, final Random rng) {
      result.z = rng.nextDouble();
      final double r = Math.sqrt(Math.max(0., 1. - result.z * result.z));
      final double phi = 2.0 * Math.PI * rng.nextDouble();
      result.x = r * Math.cos(phi);
      result.y = r * Math.sin(phi);
      return 1.0 / (2.0 * Math.PI);
   }
   
   /**
    * Generates a vector randomly sampled from the hemisphere surrounding the z axis with a cosine probability
    * distribution.
    * 
    * @param result
    *           The resulting random sample.
    * @param rng
    *           A random number generator.
    * @return the probability of the generated ray occuring of all possible directions around the hemisphere.
    * @see PBRT implementation.
    */
   public static final double cosSampleHemisphere(final Vec3 result, final Random rng) {
      /*
       * Cosine-weighted sampling about the surface normal:
       * 
       * Probability of direction Wo = 1/pi * cos(theta) where theta is the angle between the surface normal and Ko.
       * 
       * The polar angle about the normal is chosen from a uniform distribution 0..2pi
       */
      final double cosTheta = Math.sqrt(1.0 - rng.nextDouble());
      final double sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);
      final double phi = 2.0 * Math.PI * rng.nextDouble();
      final double xb = sinTheta * Math.cos(phi);
      final double yb = sinTheta * Math.sin(phi);

      result.set(xb, yb, cosTheta);

      return cosTheta / (Math.PI);
   }

}
