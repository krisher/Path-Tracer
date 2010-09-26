package edu.rit.krisher.scene;

import java.util.Random;

import edu.rit.krisher.raytracer.rays.SampleRay;
import edu.rit.krisher.scene.material.Color;
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
    * Computes and returns a spectral response function (given the spectrum of
    * incident light, returns the reflected/transmitted spectrum) given a sample
    * direction, incident light direction, surface normal, and material specific
    * coordinates of the reflection/transmission point. This is effectively a
    * BRDF/BTDF function.
    * 
    * <p>
    * The returned value represents the percentage of the incoming radiant intensity (W/sr) that is transmitted, so
    * Color component values must range from 0 to 1.<br>
    * 
    * TODO: return a spectral response function instead of a color for improved color accuracy and support for
    * fluorescence.
    * 
    * @param colorOut
    *           the resulting color.
    * @param sampleDirection
    *           A normalized vector toward the material intersection point from
    *           the point where the transmitted/reflected spectrum is being
    *           sampled.
    * @param rng
    *           TODO
    * @param incidentLightDirection
    *           A normalized vector toward the incident light source from the
    *           material intersection point.
    * @param surfaceNormal
    *           The surface normal of the geometry where the intersection
    *           occurred.
    * @param materialCoords
    *           Material specific coordinates.
    **/
   public void getDirectIlluminationTransport(Color colorOut, Vec3 sampleDirection, Random rng,
         Vec3 incidentLightDirection, Vec3 surfaceNormal, double... materialCoords);

   /**
    * Computes and returns the emissive color given the specified sample
    * direction, surface normal and material coordinates.
    * 
    * Emissive color contribution is independent of any irradiance at the
    * intersection point parameters. This can be used to represent light source
    * emission, materials that don't interact with light (e.g. simple color
    * materials), and ambient lighting terms.
    * 
    * The returned value represents the radiant intensity of the source over the
    * visible spectrum, in Watts/Steradian. The Color components may range from
    * 0 to positive infinity.
    * 
    * @param sampleDirection
    *           A normalized vector pointing toward the material intersection
    *           (the opposite direction as the light that will be emitted)
    * @param surfaceNormal
    *           The surface normal at the point where the sample ray intersects
    *           the geometry.
    * @param materialCoords
    *           A material-specific array of coordinates.
    * @return The color that is emitted back toward the sample source (in the
    *         opposite direction as the sampleDirection).
    */
   public void getEmissionColor(Color emissionOut, Vec3 sampleDirection, Vec3 surfaceNormal, double... materialCoords);

   /**
    * Accessor to determine whether this material should be sampled for its
    * response to direct illumination (shadow rays) or not. Certain materials
    * (such as highly specular materials) will only respond to direct
    * illumination in a very small portion of the reflectance hemisphere, which
    * will typically be well sampled through other means.
    * 
    * @return
    */
   public boolean shouldSampleDirectIllumination();

   /**
    * Computes a set of directions that should be sampled for irradiance.
    * 
    * @param outDirections
    *           A non-null array to store the resulting sample directions. All
    *           vectors placed in this array should be normalized. The length of
    *           the array determines the maximum number of samples that should
    *           be generated.
    * @param rng
    *           A random number generator for monte-carlo sampling, etc.
    * @param radianceSampleDirection
    *           The normalized vector indicating the direction that the
    *           irradiance samples will be transmitted (this points toward the
    *           material intersection point).
    * @param surfaceNormal
    *           The surface normal of the geometry where the sample is being
    *           taken.
    * @param materialCoords
    *           The material coordinates of the sample.
    * @return a non-negative integer indicating the number of samples returned
    *         in out.
    */
   public void sampleIrradiance(SampleRay sampleOut, Random rng, Vec3 radianceSampleDirection, Vec3 surfaceNormal,
         double... materialCoords);
}
