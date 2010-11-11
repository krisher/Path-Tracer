package edu.rit.krisher.scene;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.IntersectionInfo;
import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Ray;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Material encapsulates a spectral response to incoming light in a particular
 * outgoing direction (i.e. a BRDF/BTDF), direct emission of light in a
 * particular direction, and a distribution function that can be sampled
 * 
 * Note that the emissive component does not imply that the geometry that has a
 * material emits light, the geometry must also implement {@link EmissiveGeometry} for this to happen.
 * 
 * @author krisher
 * 
 */
public interface Material {

   /**
    * Given the provided incident light direction, and given incident light power distribution, compute the exitant
    * light power distribution in the provided exitant direction.
    * 
    * <p>
    * The returned value represents the percentage of the incoming radiant intensity (W/sr) that is transmitted, so
    * Color component values must range from 0 to 1.<br>
    * 
    * @param throughput
    *           On input, this is the color of the light incident on the material, on output, stores the resulting color
    *           response. Note that this method should not take the incident light angle into account (i.e. the
    *           cos(theta_i)), this will be handled externally.
    * @param wo
    *           A normalized vector away from the material intersection point.
    * @param wi
    *           A normalized vector away from the material intersection point.
    * @param parameters
    *           Material parameters including surface normal of the geometry where the intersection occurred, and
    *           texture/material coordinates.
    **/
   public void evaluateBRDF(Color throughput, Vec3 wo, Vec3 wi, IntersectionInfo parameters);

   /**
    * Computes and returns the emitted (not reflected) light toward the origin of wo, along the wo ray direction.
    * <p>
    * The returned value represents the radiant intensity of the source over the visible spectrum, in Watts/Steradian.
    * The Color components may range from 0 to positive infinity.
    * 
    * @param emissionOut
    *           out variable used to store the color that is emitted toward the illuminated point.
    * @param wo
    *           A ray from the illuminated point toward the intersection with the material.
    * @param parameters
    *           Material parameters including surface normal of the geometry where the intersection occurred, and
    *           texture/material coordinates.
    */
   public void getEmissionColor(Color emissionOut, Ray wo, IntersectionInfo parameters);

   /**
    * Returns true if there is any chance of transmission/reflection of light that is not very close to the perfect
    * mirror reflection direction.
    * 
    * @return true if direct illuminant sampling should be performed for this material (in addition to casting secondary
    *         rays), false if only the secondary rays should be processed.
    */
   public boolean isDiffuse();

   /**
    * Computes a direction that should be sampled.
    * 
    * @param wi
    *           Ray to initialize to the sample direction. Upon exit, the ray's direction will be initialized to an
    *           incident light direction to sample (pointing away from the BRDF origin), and the throughput will be initialized to
    *           the reflective power throughput in the specified direction.
    * @param wo
    *           Vector indicating the outgoing reflection direction (pointing away from the BRDF origin).
    * @param intersectionInfo
    *           Material parameters including surface normal of the geometry where the intersection occurred, and
    *           texture/material coordinates.
    * @param rng
    *           A random number generator for Monte Carlo sampling.
    * @return The probability (PDF) of the specified direction.
    */
   public double sampleBRDF(SampleRay wi, Vec3 wo, IntersectionInfo intersectionInfo, Random rng);

   // public SampleRay[] multisampleBRDF(int sampleCount, Vec3 wi, IntersectionInfo parameters, Random rng);
}
