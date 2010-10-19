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
    * Computes and returns a spectral response function (given the spectrum of incident light, returns the
    * reflected/transmitted spectrum) given a sample direction, incident light direction, surface normal, and material
    * specific coordinates of the reflection/transmission point. This is effectively a BRDF/BTDF function.
    * 
    * <p>
    * The returned value represents the percentage of the incoming radiant intensity (W/sr) that is transmitted, so
    * Color component values must range from 0 to 1.<br>
    * 
    * TODO: return a spectral response function instead of a color for improved color accuracy and support for
    * fluorescence.
    * 
    * @param colorInOut
    *           On input, this is the color of the light incident on the material, on output, stores the resulting color
    *           response.
    * @param wOutgoing
    *           A normalized vector toward the material intersection point from the point where the
    *           transmitted/reflected spectrum is being sampled.
    * @param wIncoming
    *           A normalized vector toward the incident light source from the material intersection point.
    * @param parameters
    *           Material parameters including surface normal of the geometry where the intersection occurred, and
    *           texture/material coordinates.
    **/
   public void getIrradianceResponse(Color colorInOut, Vec3 wOutgoing, Vec3 wIncoming, MaterialInfo parameters);

   /**
    * Computes and returns the emissive color given the specified sample direction, surface normal and material
    * coordinates.
    * <p>
    * Emissive color contribution is independent of any irradiance at the intersection point parameters. This can be
    * used to represent light source emission, materials that don't interact with light (e.g. simple color materials),
    * and ambient lighting terms.
    * <p>
    * The returned value represents the radiant intensity of the source over the visible spectrum, in Watts/Steradian.
    * The Color components may range from 0 to positive infinity.
    * 
    * @param emissionOut
    *           out variable (non-null) used to store the color that is emitted back toward the sample source (in the
    *           opposite direction as the sampleDirection).
    * @param sampleDirection
    *           A normalized vector pointing toward the material intersection (the opposite direction as the light that
    *           will be emitted)
    * @param parameters
    *           Material parameters including surface normal of the geometry where the intersection occurred, and
    *           texture/material coordinates.
    */
   public void getEmissionColor(Color emissionOut, Vec3 sampleDirection, MaterialInfo parameters);

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
    * @param sampleOut
    *           Ray to store the desired sample direction in.
    * @param rng
    *           A random number generator for monte-carlo sampling, etc.
    * @param wIncoming
    *           The normalized vector indicating the direction that the irradiance samples will be transmitted (this
    *           points toward the material intersection point). This vector must not be modified in any way.
    * @param parameters
    *           Material parameters including surface normal of the geometry where the intersection occurred, and
    *           texture/material coordinates.
    */
   public void samplePDF(SampleRay sampleOut, Random rng, Vec3 wIncoming, MaterialInfo parameters);
}
