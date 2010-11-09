package edu.rit.krisher.scene;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;

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
    *           The ray whose origin indicates the illuminated point, the direction vector, spectral power
    *           distribution and intersection info will be updated by this method.
    * @param rng
    *           A non-null random number generator that can be used for generating uniform random variables.
    */
   void sampleEmissiveRadiance(SampleRay wo, Random rng);

}
