package edu.rit.krisher.scene;

import java.util.Random;

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
    * @param rng
    *           A non-null random number generator that can be used for generating uniform random variables.
    * @return The number of samples generated.
    */
   int sampleIrradiance(SampleRay[] wo, Vec3 point, Random rng);

   /**
    * Initializes the origin, direction, and sampleColor (radiance) in each of the wo SampleRays. Note that the origin
    * should be perturbed slightly (e.g. by {@link Constants#EPSILON_D}) along the ray direction in order to ensure that
    * the ray does not intersect with the light at the ray's origin with distance > 0 (due to precision errors).
    * 
    * @param wo
    *           An array of SampleRays to initialize.
    * @param woOffset
    *           The index in wo at which to start initializing samples.
    * @param woCount
    *           The maximum number of samples to generate.
    * @param rng
    *           A Random number generator for sampling.
    * @return the number of rays in wo that were actually initialized.
    */
   int sampleEmission(SampleRay[] wo, int woOffset, int woCount, Random rng);
}
