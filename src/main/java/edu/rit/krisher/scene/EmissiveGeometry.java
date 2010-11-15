package edu.rit.krisher.scene;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.vecmath.Constants;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Interface for geometric objects that emit light, and support direct illumination sampling.
 * 
 * @author krisher
 * 
 */
public interface EmissiveGeometry extends Geometry {

   /**
    * Samples the power distribution of the light emitted toward a specified point. The provided Ray's origin indicates
    * a point that may be illuminated by this light source (barring any occlusion from other geometry). This method will
    * update the ray's direction vector to point toward a randomly sampled location on the light surface, and return the
    * spectral power distribution emitted from the light to the ray origin via the supplied parameter.
    * 
    * @param wo
    *           The ray whose origin indicates the illuminated point, the direction vector, spectral power distribution
    *           and intersection info will be updated by this method.
    * @param r1
    *           A random number, assumed to be from a uniform distribution.
    * @param r2
    *           A random number, assumed to be from a uniform distribution.
    * @return The number of samples generated.
    */
   void sampleIrradiance(SampleRay wo, Vec3 point, float r1, float r2);

   /**
    * Initializes the origin, direction, and sampleColor (radiance) in the wo SampleRay. Note that the origin should be
    * perturbed slightly (e.g. by {@link Constants#EPSILON_D}) along the ray direction in order to ensure that the ray
    * does not intersect with the light at the ray's origin with distance > 0 (due to precision errors).
    * 
    * @param wo
    *           A SampleRay to initialize.
    * @param r1
    *           A uniform random number used for Monte Carlo sampling
    * @param r2
    *           A uniform random number used for Monte Carlo sampling
    */
   void sampleEmission(SampleRay wo, float r1, float r2);
}
