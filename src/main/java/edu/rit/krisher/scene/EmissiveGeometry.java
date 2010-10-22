package edu.rit.krisher.scene;

import java.util.Random;

import edu.rit.krisher.scene.material.Color;
import edu.rit.krisher.vecmath.Vec3;

/**
 * Interface for geometric objects that emit light, and support direct
 * illumination sampling.
 * 
 * @author krisher
 * 
 */
public interface EmissiveGeometry extends Geometry {

   /**
    * Given an arbitrary origin point in world-space, compute a vector that
    * intersects the light source geometry at a randomly sampled point.
    * 
    * @param directionOut
    *           The non-null vector in which to store the (normalized) result.
    * @param radianceOut
    *           The light energy that is transmitted along the sample ray.
    * @param origin
    *           The non-null point from which the sample is taken
    * @param rng
    *           A non-null random number generator that can be used for
    *           generating uniform random variables.
    * @return this distance to the sample point.
    */
   double sampleEmissiveRadiance(Vec3 directionOut, Color radianceOut,  Vec3 origin, Random rng);

}
